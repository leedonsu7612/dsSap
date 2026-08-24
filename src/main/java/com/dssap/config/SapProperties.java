package com.dssap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "sap")
public class SapProperties {

    /**
     * mock: 로컬 샘플 데이터
     * jco: SAP Java Connector RFC (실시간/배치 모두 사용)
     * odata: SAP Gateway / S/4HANA OData
     */
    private String client = "mock";
    private String systemId = "DS1";
    private String language = "KO";
    private Jco jco = new Jco();
    private Odata odata = new Odata();
    private Realtime realtime = new Realtime();
    private Batch batch = new Batch();

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Jco getJco() {
        return jco;
    }

    public void setJco(Jco jco) {
        this.jco = jco;
    }

    public Odata getOdata() {
        return odata;
    }

    public void setOdata(Odata odata) {
        this.odata = odata;
    }

    public Realtime getRealtime() {
        return realtime;
    }

    public void setRealtime(Realtime realtime) {
        this.realtime = realtime;
    }

    public Batch getBatch() {
        return batch;
    }

    public void setBatch(Batch batch) {
        this.batch = batch;
    }

    public static class Jco {
        private String ashost;
        private String sysnr = "00";
        private String client = "100";
        private String user;
        private String passwd;
        private String lang = "KO";
        private int poolCapacity = 5;
        private int peakLimit = 10;
        private String destinationName = "DS_SAP";

        public String getAshost() {
            return ashost;
        }

        public void setAshost(String ashost) {
            this.ashost = ashost;
        }

        public String getSysnr() {
            return sysnr;
        }

        public void setSysnr(String sysnr) {
            this.sysnr = sysnr;
        }

        public String getClient() {
            return client;
        }

        public void setClient(String client) {
            this.client = client;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getPasswd() {
            return passwd;
        }

        public void setPasswd(String passwd) {
            this.passwd = passwd;
        }

        public String getLang() {
            return lang;
        }

        public void setLang(String lang) {
            this.lang = lang;
        }

        public int getPoolCapacity() {
            return poolCapacity;
        }

        public void setPoolCapacity(int poolCapacity) {
            this.poolCapacity = poolCapacity;
        }

        public int getPeakLimit() {
            return peakLimit;
        }

        public void setPeakLimit(int peakLimit) {
            this.peakLimit = peakLimit;
        }

        public String getDestinationName() {
            return destinationName;
        }

        public void setDestinationName(String destinationName) {
            this.destinationName = destinationName;
        }
    }

    public static class Odata {
        private String baseUrl;
        private String username;
        private String password;
        private String sapClient = "100";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getSapClient() {
            return sapClient;
        }

        public void setSapClient(String sapClient) {
            this.sapClient = sapClient;
        }
    }

    public static class Realtime {
        private long timeoutMs = 10_000;
        private String materialBapi = "BAPI_MATERIAL_GET_DETAIL";
        private String salesOrderBapi = "BAPI_SALESORDER_GETLIST";

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public String getMaterialBapi() {
            return materialBapi;
        }

        public void setMaterialBapi(String materialBapi) {
            this.materialBapi = materialBapi;
        }

        public String getSalesOrderBapi() {
            return salesOrderBapi;
        }

        public void setSalesOrderBapi(String salesOrderBapi) {
            this.salesOrderBapi = salesOrderBapi;
        }
    }

    public static class Batch {
        private boolean enabled = true;
        private String cron = "0 0 2 * * *";
        private int chunkSize = 200;
        private int pageSize = 500;
        private String functionName = "RFC_READ_TABLE";
        private String tableName = "MARA";
        private List<String> fields = new ArrayList<>();
        private String where = "";
        private String outputDir = "./data/batch-out";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public int getChunkSize() {
            return chunkSize;
        }

        public void setChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        public String getFunctionName() {
            return functionName;
        }

        public void setFunctionName(String functionName) {
            this.functionName = functionName;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public List<String> getFields() {
            return fields;
        }

        public void setFields(List<String> fields) {
            this.fields = fields;
        }

        public String getWhere() {
            return where;
        }

        public void setWhere(String where) {
            this.where = where;
        }

        public String getOutputDir() {
            return outputDir;
        }

        public void setOutputDir(String outputDir) {
            this.outputDir = outputDir;
        }
    }
}
