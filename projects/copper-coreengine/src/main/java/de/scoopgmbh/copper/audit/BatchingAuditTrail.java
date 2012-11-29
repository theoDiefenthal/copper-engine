/*
 * Copyright 2002-2012 SCOOP Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.scoopgmbh.copper.audit;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.support.JdbcUtils;

import de.scoopgmbh.copper.batcher.Batcher;
import de.scoopgmbh.copper.batcher.CommandCallback;
import de.scoopgmbh.copper.db.utility.RetryingTransaction;
import de.scoopgmbh.copper.management.AuditTrailMXBean;

/**
 * Fast db based audit trail implementation. It is possible to extend the COPPER audit trail with custom attributes. See JUnitTest
 * {@link BatchingAuditTrailTest#testCustomTable()} for an example.
 * 
 * @author austermann
 *
 */
public class BatchingAuditTrail implements AuditTrail, AuditTrailMXBean, InitializingBean {
	
	private static final Logger logger = LoggerFactory.getLogger(BatchingAuditTrail.class);
	
	public static final class Property2ColumnMapping {
		String columnName;
		String propertyName;

		public Property2ColumnMapping() {
		}
		
		public Property2ColumnMapping(String propertyName,String columnName) {
			this.columnName = columnName;
			this.propertyName = propertyName;
		}

		public String getColumnName() {
			return columnName;
		}
		public String getPropertyName() {
			return propertyName;
		}
		public void setColumnName(String columnName) {
			this.columnName = columnName;
		}
		public void setPropertyName(String propertyName) {
			this.propertyName = propertyName;
		}
	}

	private Batcher batcher;
	private DataSource dataSource;
	private int level = 5;
	private MessagePostProcessor messagePostProcessor = new DummyPostProcessor();
	private Class<?> auditTrailEventClass;
	private String dbTable = "COP_AUDIT_TRAIL_EVENT";
	private List<Property2ColumnMapping> mapping;

	private final List<Method> propertyGetters = new ArrayList<Method>();
	private boolean isOracle;
	private String sqlStmt;
	
	public BatchingAuditTrail() {
		mapping = createDefaultMapping();
		auditTrailEventClass = AuditTrailEvent.class;
	}

	public static List<Property2ColumnMapping> createDefaultMapping() {
		List<Property2ColumnMapping> mapping = new ArrayList<BatchingAuditTrail.Property2ColumnMapping>();
		mapping.add(new Property2ColumnMapping("logLevel", "LOGLEVEL"));
		mapping.add(new Property2ColumnMapping("occurrence", "OCCURRENCE"));
		mapping.add(new Property2ColumnMapping("conversationId", "CONVERSATION_ID"));
		mapping.add(new Property2ColumnMapping("context", "CONTEXT"));
		mapping.add(new Property2ColumnMapping("instanceId", "INSTANCE_ID"));
		mapping.add(new Property2ColumnMapping("correlationId", "CORRELATION_ID"));
		mapping.add(new Property2ColumnMapping("transactionId", "TRANSACTION_ID"));
		mapping.add(new Property2ColumnMapping("messageType", "MESSAGE_TYPE"));
		mapping.add(new Property2ColumnMapping("message", "LONG_MESSAGE"));
		return mapping;
	}

	public void setMessagePostProcessor(MessagePostProcessor messagePostProcessor) {
		this.messagePostProcessor = messagePostProcessor;
	}

	public void setBatcher(Batcher batcher) {
		this.batcher = batcher;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public void setLevel (int level) {
		this.level = level;
	}
	
	public void setAuditTrailEventClass(Class<?> auditTrailEventClass) {
		this.auditTrailEventClass = auditTrailEventClass;
	}
	
	public void setDbTable(String dbTable) {
		this.dbTable = dbTable;
	}

	public void setMapping(List<Property2ColumnMapping> mapping) {
		this.mapping = mapping;
	}

	public void setAdditionalMapping(List<Property2ColumnMapping> mapping) {
		ArrayList<Property2ColumnMapping> newMapping = new ArrayList<BatchingAuditTrail.Property2ColumnMapping>();
		newMapping.addAll(mapping);
		newMapping.addAll(this.mapping);
		this.mapping = newMapping;
	}
	
	public void startup() throws Exception {
		logger.info("Starting up...");
		final Connection con = dataSource.getConnection();
		try {
			isOracle = con.getMetaData().getDatabaseProductName().equalsIgnoreCase("oracle");
		}
		finally {
			JdbcUtils.closeConnection(con);
		}
		
		sqlStmt = createSqlStmt();
		
	}

	private String createSqlStmt() throws IntrospectionException {
		final BeanInfo beanInfo = Introspector.getBeanInfo(auditTrailEventClass);
		final StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO ").append(dbTable).append(" (");
		int numbOfParams = 0;
		if (isOracle) {
			sql.append("SEQ_ID");
			numbOfParams++;
		}
		for (Property2ColumnMapping entry : mapping) {
			if (numbOfParams > 0) {
				sql.append(",");
			}
			sql.append(entry.getColumnName());
			
			boolean found = false;
			for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
				if (pd.getName().equals(entry.getPropertyName())) {
					propertyGetters.add(pd.getReadMethod());
					found = true;
					break;
				}
			}
			if (!found) throw new IllegalArgumentException("Cannot find read method for property '"+entry.getPropertyName()+"' in class '"+auditTrailEventClass+"'");			
			numbOfParams++;
		}
		sql.append(") VALUES (");
		if (isOracle) {
			sql.append("NVL(?,COP_SEQ_AUDIT_TRAIL.NEXTVAL),");
			numbOfParams--;
		}
		for (int i=0; i<numbOfParams; i++) {
			if (i > 0) {
				sql.append(",");
			}
			sql.append("?");
		}
		sql.append(")");
		return sql.toString();
	}
	
	String getSqlStmt() {
		return sqlStmt;
	}
	
	
	@Override
	public int getLevel() {
		return level;
	}

	@Override
	public boolean isEnabled (int level) {
		return this.level >= level;
	}

	@Override
	public void synchLog(int logLevel, Date occurrence, String conversationId, String context, String instanceId, String correlationId, String transactionId, String _message, String messageType) {
		this.synchLog(new AuditTrailEvent(logLevel, occurrence, conversationId, context, instanceId, correlationId, transactionId, _message, messageType, null));

	}

	@Override
	public void asynchLog(int logLevel, Date occurrence, String conversationId, String context, String instanceId, String correlationId, String transactionId, String _message, String messageType) {
		this.asynchLog(new AuditTrailEvent(logLevel, occurrence, conversationId, context, instanceId, correlationId, transactionId, _message, messageType, null));
	}

	@Override
	public void asynchLog(int logLevel, Date occurrence, String conversationId, String context, String instanceId, String correlationId, String transactionId, String _message, String messageType, final AuditTrailCallback cb) {
		this.asynchLog(new AuditTrailEvent(logLevel, occurrence, conversationId, context, instanceId, correlationId, transactionId, _message, messageType, null), cb);
	}

	@Override
	public void asynchLog(AuditTrailEvent e) {
		if ( isEnabled(e.logLevel) ) {
			e.setMessage(messagePostProcessor.serialize(e.message));
			CommandCallback<BatchInsertIntoAutoTrail.Command> callback = new CommandCallback<BatchInsertIntoAutoTrail.Command>() {
				@Override
				public void commandCompleted() {
				}
				@Override
				public void unhandledException(Exception e) {
				}
			};
			batcher.submitBatchCommand(new BatchInsertIntoAutoTrail.Command(e, isOracle, sqlStmt, propertyGetters, callback));
		}		
	}

	@Override
	public void asynchLog(final AuditTrailEvent e, final AuditTrailCallback cb) {
		if ( isEnabled(e.logLevel) ) {
			e.setMessage(messagePostProcessor.serialize(e.message));
			CommandCallback<BatchInsertIntoAutoTrail.Command> callback = new CommandCallback<BatchInsertIntoAutoTrail.Command>() {
				@Override
				public void commandCompleted() {
					cb.done();
				}
				@Override
				public void unhandledException(Exception e) {
					cb.error(e);
				}
			};
			batcher.submitBatchCommand(new BatchInsertIntoAutoTrail.Command(e, isOracle, sqlStmt, propertyGetters, callback));
		}		
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void doSyncLog(AuditTrailEvent e, Connection con) throws Exception {
		e.setMessage(messagePostProcessor.serialize(e.message));
		BatchInsertIntoAutoTrail.Command cmd = new BatchInsertIntoAutoTrail.Command(e, isOracle, sqlStmt, propertyGetters);
		cmd.executor().doExec((Collection)Collections.singletonList(cmd), con);
	}

	@Override
	public void synchLog(final AuditTrailEvent event) {
		if (isEnabled(event.logLevel) ) {
			try {
				new RetryingTransaction(dataSource) {
					@Override
					protected void execute() throws Exception {
						doSyncLog(event, getConnection());
					}
				}.run();
			}
			catch(RuntimeException e) {
				throw e;
			}
			catch(Exception e) {
				throw new RuntimeException("synchLog failed",e);
			}
		}
	}
	
	protected DataSource getDataSource() {
		return dataSource;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		startup();
	}

}
