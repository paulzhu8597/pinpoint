/*
 * Copyright 2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.profiler.context;

import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.context.AsyncTraceId;
import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.ParsingResult;
import com.navercorp.pinpoint.bootstrap.context.ServerMetaDataHolder;
import com.navercorp.pinpoint.bootstrap.context.Trace;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.context.TraceId;
import com.navercorp.pinpoint.bootstrap.plugin.jdbc.JdbcContext;
import com.navercorp.pinpoint.common.annotations.InterfaceAudience;
import com.navercorp.pinpoint.common.util.Assert;
import com.navercorp.pinpoint.profiler.AgentInformation;

import com.navercorp.pinpoint.profiler.context.id.TraceIdFactory;
import com.navercorp.pinpoint.profiler.metadata.ApiMetaDataService;
import com.navercorp.pinpoint.profiler.metadata.JsonMetaDataService;
import com.navercorp.pinpoint.profiler.metadata.SqlMetaDataService;
import com.navercorp.pinpoint.profiler.metadata.StringMetaDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author emeroad
 * @author HyunGil Jeong
 * @author Taejin Koo
 */
public class DefaultTraceContext implements TraceContext {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final TraceIdFactory traceIdFactory;
    private final TraceFactory traceFactory;

    private final AsyncTraceContext asyncTraceContext;

    private final AgentInformation agentInformation;

    private final ApiMetaDataService apiMetaDataService;
    private final StringMetaDataService stringMetaDataService;
    private final SqlMetaDataService sqlMetaDataService;
    private final JsonMetaDataService jsonMetaDataService;

    private final ProfilerConfig profilerConfig;

    private final ServerMetaDataHolder serverMetaDataHolder;

    private final JdbcContext jdbcContext;

    public DefaultTraceContext(final ProfilerConfig profilerConfig,
                               final AgentInformation agentInformation,
                               final TraceIdFactory traceIdFactory,
                               final TraceFactory traceFactory,
                               final AsyncTraceContext asyncTraceContext,
                               final ServerMetaDataHolder serverMetaDataHolder,
                               final ApiMetaDataService apiMetaDataService,
                               final StringMetaDataService stringMetaDataService,
                               final SqlMetaDataService sqlMetaDataService,
                               final JdbcContext jdbcContext,
                               final JsonMetaDataService jsonMetaDataService
    ) {
        this.profilerConfig = Assert.requireNonNull(profilerConfig, "profilerConfig must not be null");
        this.agentInformation = Assert.requireNonNull(agentInformation, "agentInformation must not be null");
        this.serverMetaDataHolder = Assert.requireNonNull(serverMetaDataHolder, "serverMetaDataHolder must not be null");

        this.traceIdFactory = Assert.requireNonNull(traceIdFactory, "traceIdFactory must not be null");
        this.traceFactory = Assert.requireNonNull(traceFactory, "traceFactory must not be null");
        this.asyncTraceContext = Assert.requireNonNull(asyncTraceContext, "asyncTraceContextProvider must not be null");

        this.jdbcContext = Assert.requireNonNull(jdbcContext, "jdbcContext must not be null");

        this.apiMetaDataService = Assert.requireNonNull(apiMetaDataService, "apiMetaDataService must not be null");
        this.stringMetaDataService = Assert.requireNonNull(stringMetaDataService, "stringMetaDataService must not be null");
        this.sqlMetaDataService = Assert.requireNonNull(sqlMetaDataService, "sqlMetaDataService must not be null");
        this.jsonMetaDataService = Assert.requireNonNull(jsonMetaDataService, "jsonMetaDataService must not be null");
    }

    /**
     * Return trace only if current transaction can be sampled.
     *
     * @return
     */
    public Trace currentTraceObject() {
        return traceFactory.currentTraceObject();
    }

    /**
     * Return trace without sampling check.
     *
     * @return
     */
    @Override
    public Trace currentRawTraceObject() {
        return traceFactory.currentRawTraceObject();
    }

    @Override
    public Trace disableSampling() {
        // return null; is bug. #93
        return traceFactory.disableSampling();
    }


    @Override
    public ProfilerConfig getProfilerConfig() {
        return profilerConfig;
    }

    @Override
    public Trace continueTraceObject(final TraceId traceId) {
        return traceFactory.continueTraceObject(traceId);
    }


    @Override
    public Trace continueTraceObject(Trace trace) {
        return traceFactory.continueTraceObject(trace);
    }


    @Override
    public Trace newTraceObject() {
        return traceFactory.newTraceObject();
    }

    @InterfaceAudience.LimitedPrivate("vert.x")
    @Override
    public Trace newAsyncTraceObject() {
        return traceFactory.newAsyncTraceObject();
    }

    @InterfaceAudience.LimitedPrivate("vert.x")
    @Override
    public Trace continueAsyncTraceObject(final TraceId traceId) {
        return traceFactory.continueAsyncTraceObject(traceId);
    }

    @Override
    public Trace continueAsyncTraceObject(AsyncTraceId asyncTraceId, int asyncId, long startTime) {
        return asyncTraceContext.continueAsyncTraceObject(asyncTraceId, asyncId, startTime).get();
    }


    @Override
    public Trace removeTraceObject() {
        return removeTraceObject(true);
    }

    @Override
    public Trace removeTraceObject(boolean closeUnsampledTrace) {
        final Trace trace = traceFactory.removeTraceObject();
        if (closeUnsampledTrace) {
            return closeUnsampledTrace(trace);
        } else {
            return trace;
        }
    }

    private Trace closeUnsampledTrace(Trace trace) {
        if (trace == null) {
            return null;
        }
        // work around : unsampled trace must be closed.
        if (!trace.canSampled()) {
            if (!trace.isClosed()) {
                trace.close();
            }
        }
        return trace;
    }

    @Override
    public String getAgentId() {
        return this.agentInformation.getAgentId();
    }

    @Override
    public String getApplicationName() {
        return this.agentInformation.getApplicationName();
    }

    @Override
    public long getAgentStartTime() {
        return this.agentInformation.getStartTime();
    }

    @Override
    public short getServerTypeCode() {
        return this.agentInformation.getServerType().getCode();
    }

    @Override
    public String getServerType() {
        return this.agentInformation.getServerType().getDesc();
    }

    @Override
    public int cacheApi(final MethodDescriptor methodDescriptor) {
        return this.apiMetaDataService.cacheApi(methodDescriptor);
    }

    @Override
    public int cacheString(final String value) {
        return this.stringMetaDataService.cacheString(value);
    }

    @Override
    public TraceId createTraceId(final String transactionId, final long parentSpanId, final long spanId, final short flags) {
        if (transactionId == null) {
            throw new NullPointerException("transactionId must not be null");
        }
        // TODO Should handle exception when parsing failed.
        return traceIdFactory.continueTraceId(transactionId, parentSpanId, spanId, flags);
    }

    @Override
    public ParsingResult parseSql(final String sql) {
        return this.sqlMetaDataService.parseSql(sql);
    }

    @Override
    public boolean cacheSql(ParsingResult parsingResult) {
        return this.sqlMetaDataService.cacheSql(parsingResult);
    }

    @Override
    public ParsingResult parseJson(final String json) {
        return this.jsonMetaDataService.parseJson(json);
    }

    @Override
    public ServerMetaDataHolder getServerMetaDataHolder() {
        return this.serverMetaDataHolder;
    }

    @Override
    public int getAsyncId() {
        return this.asyncTraceContext.nextAsyncId();
    }

    @Override
    public JdbcContext getJdbcContext() {
        return jdbcContext;
    }

}
