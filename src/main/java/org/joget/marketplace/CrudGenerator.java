package org.joget.marketplace;

import com.google.gson.Gson;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.joget.apps.app.dao.EnvironmentVariableDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.EnvironmentVariable;
import org.joget.apps.app.model.FormDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.generator.model.GeneratorResult;
import org.joget.apps.generator.service.GeneratorUtil;
import org.joget.commons.util.LogUtil;
import org.joget.marketplace.model.ColumnProperties;
import org.joget.marketplace.model.FormElement;
import org.joget.marketplace.model.JogetForm;
import org.joget.marketplace.model.LoadBinder;
import org.joget.marketplace.model.GeneralProperties;
import org.joget.marketplace.model.FieldProperties;
import org.joget.marketplace.model.LoadBinderProperties;
import org.joget.marketplace.model.MetaData;
import org.joget.marketplace.model.SectionElement;
import org.joget.marketplace.model.SectionProperties;
import org.joget.marketplace.model.StoreBinder;
import org.joget.marketplace.model.StoreBinderProperties;
import org.joget.marketplace.model.Validator;
import org.joget.marketplace.model.ValidatorProperties;
import org.springframework.context.ApplicationContext;
import org.apache.commons.dbcp2.BasicDataSourceFactory;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.commons.util.DynamicDataSourceManager;
import org.joget.plugin.base.PluginWebSupport;
import org.json.JSONException;
import org.json.JSONObject;


public class CrudGenerator extends WorkflowFormBinder implements PluginWebSupport {
    private static final String MESSAGE_PATH = "message/CrudGenerator";

    @Override
    public FormRowSet store(Element element, FormRowSet rows, FormData formData) {
        String datasourceType = formData.getRequestParameter(getPropertyString("datasourcefield"));
        String tableName = formData.getRequestParameter(getPropertyString("tablenamefield"));
        String keyColumn = formData.getRequestParameter(getPropertyString("keycolumnfield"));
        String customJDBCDriver = formData.getRequestParameter(getPropertyString("driverfield"));
        String customJDBCURL = formData.getRequestParameter(getPropertyString("urlfield"));
        String customJDBCUsername = formData.getRequestParameter(getPropertyString("usernamefield"));
        String customJDBCPassword = formData.getRequestParameter(getPropertyString("passwordfield"));
    
        ArrayList<String> customJDBCArr = new ArrayList<String>();
        
        customJDBCArr.add(customJDBCDriver);
        customJDBCArr.add(customJDBCURL);
        customJDBCArr.add(customJDBCUsername);
        customJDBCArr.add(customJDBCPassword);

        List<MetaData> metaDataList = new ArrayList<>();
        DataSource ds = null;
        Connection con = null;
        try {
            ds = createDataSource(datasourceType, customJDBCArr);
            con = ds.getConnection();
            String sql = "SELECT * FROM " + tableName;
            PreparedStatement stmt = con.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            String result = "";
            ResultSetMetaData metaData = stmt.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                MetaData md = new MetaData();
                md.setName(metaData.getColumnName(i));
                md.setLabel(metaData.getColumnLabel(i));
                md.setType(metaData.getColumnTypeName(i));
                metaDataList.add(md);
            }
            List<FormElement> formElementList = generateForm(datasourceType, tableName, keyColumn, customJDBCArr, metaDataList);
            generateCrud(tableName, formElementList);
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        } finally {
            //always close the connection after used
            try {
                if (con != null && !con.isClosed()) {
                    con.close();
                }
            } catch (SQLException e) {/* ignored */
            }
        }

        FormRowSet frs = super.store(element, rows, formData);
        return frs;
    }

    protected DataSource createDataSource(String datasourceType, ArrayList<String> customJDBCArr) throws Exception{
        DataSource ds = null;
        if ("default".equals(datasourceType)) {
            // use current datasource
            ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        } else {
            // use custom datasource
            Properties dsProps = new Properties();
            dsProps.put("driverClassName", customJDBCArr.get(0));
            dsProps.put("url", customJDBCArr.get(1));
            dsProps.put("username", customJDBCArr.get(2));
            dsProps.put("password", customJDBCArr.get(3));
            ds = BasicDataSourceFactory.createDataSource(dsProps);
        }
        return ds;
    }

    public void generateCrud(String formId, List<FormElement> formElementList) {
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        org.joget.plugin.enterprise.CrudGenerator generator = new org.joget.plugin.enterprise.CrudGenerator();
        generator.setAppDefinition(appDef);
        generator.setFormId(formId);

        String userviewId = GeneratorUtil.getFirstAvailableUserviewId(appDef);
        generator.setProperty("userviewId", userviewId);

        generator.setProperty("newListId", formId + "_list");
        generator.setProperty("newListName", formId + "_list");
        StringBuilder sb = new StringBuilder();
        for (FormElement col : formElementList) {
            String colId = col.getProperties().getId();
            if (colId.startsWith("c_")) {
                colId = colId.substring(2);
            }
            sb.append(colId).append(";");
        }

        generator.setProperty("categoryLabel", "crud_"+formId);
        generator.setProperty("newListColumns", sb.toString());
        generator.setProperty("menuId", "menu_"+formId);
        generator.setProperty("menuLabel", formId);
        generator.setProperty("showRowCount", "true");
        generator.setProperty("showDelete", "true");
        
        ApplicationContext appContext = AppUtil.getApplicationContext();
        EnvironmentVariableDao environmentVariableDao = (EnvironmentVariableDao) appContext.getBean("environmentVariableDao");
        EnvironmentVariable env = environmentVariableDao.loadById("redirectUrl", appDef);
        if (env != null) {
            env.setValue("menu_"+formId);
        }
        
        GeneratorResult generatorResult = generator.generate();
        String linkMessage = generatorResult.getMessage();
    }

    public String getLaunchUrl(String linkMessage) {
        Pattern pattern = Pattern.compile("<a[^>]*>(.*?)</a>");
        Matcher matcher = pattern.matcher(linkMessage);
        String[] extractedTags = new String[2];
        int index = 0;
        while (matcher.find()) {
            String tag = matcher.group();
            extractedTags[index] = tag;
            index++;
        }
        return extractedTags[1];
    }

    public List<FormElement> generateForm(String datasourceType, String tableName, String keyColumn, ArrayList<String> customJDBCArr, List<MetaData> metaDataList) {
        JogetForm jogetForm = new JogetForm();
        jogetForm.setClassName("org.joget.apps.form.model.Form");
        GeneralProperties props = new GeneralProperties();
        props.setId(tableName);
        props.setName(tableName.toUpperCase());
        props.setTableName(tableName);

        LoadBinder lb = new LoadBinder();
        lb.setClassName("org.joget.plugin.enterprise.DatabaseWizardLoadBinder");
        LoadBinderProperties lbProperties = new LoadBinderProperties();
        lbProperties.setAutoHandleWorkflowVariable("true");
        lbProperties.setAutoHandleFiles("");
        lbProperties.setTableName(tableName);
        lbProperties.setKeyColumn(keyColumn);
        lbProperties.setExtraCondition("");
        if (datasourceType.equals("custom")) {
            lbProperties.setJdbcDatasource(datasourceType);
            lbProperties.setJdbcDriver(customJDBCArr.get(0));
            lbProperties.setJdbcUrl(customJDBCArr.get(1));
            lbProperties.setJdbcUser(customJDBCArr.get(2));
            lbProperties.setJdbcPassword(customJDBCArr.get(3));
        } else if (datasourceType.equals("default")) {
            lbProperties.setJdbcDatasource(datasourceType);
        }
        lb.setProperties(lbProperties);
        props.setLoadBinder(lb);

        StoreBinder sb = new StoreBinder();
        sb.setClassName("org.joget.plugin.enterprise.DatabaseWizardStoreBinder");
        StoreBinderProperties sbProperties = new StoreBinderProperties();
        sbProperties.setAutoHandleWorkflowVariable("true");
        sbProperties.setAutoHandleFiles("");
        sbProperties.setTableName(tableName);
        sbProperties.setKeyColumn(keyColumn);
        lbProperties.setExtraCondition("");
        if (datasourceType.equals("custom")) {
            sbProperties.setJdbcDatasource(datasourceType);
            sbProperties.setJdbcDriver(customJDBCArr.get(0));
            sbProperties.setJdbcUrl(customJDBCArr.get(1));
            sbProperties.setJdbcUser(customJDBCArr.get(2));
            sbProperties.setJdbcPassword(customJDBCArr.get(3));
        } else if (datasourceType.equals("default")) {
            sbProperties.setJdbcDatasource(datasourceType);
        }
        sb.setProperties(sbProperties);
        props.setStoreBinder(sb);

        props.setDescription("");
        jogetForm.setProperties(props);

        List<FormElement> formElementList = new ArrayList<>();

        for (MetaData md : metaDataList) {
            String name = md.getName();

            FormElement formElement = new FormElement();
            formElement.setClassName("org.joget.apps.form.lib.TextField");
            FieldProperties fieldProperties = new FieldProperties();
            fieldProperties.setEncryption("");
            fieldProperties.setReadonly("");
            fieldProperties.setStyle("");
            fieldProperties.setLabel(name);
            fieldProperties.setReadonlyLabel("");
            fieldProperties.setStoreNumeric("");
            fieldProperties.setId(name);
            fieldProperties.setValue("");
            fieldProperties.setMaxlength("");
            Validator validator = new Validator();
            validator.setClassName("");
            validator.setProperties(new ValidatorProperties());
            fieldProperties.setValidator(validator);
            fieldProperties.setPlaceholder("");
            fieldProperties.setSize("");
            fieldProperties.setWorkflowVariable("");
            formElement.setProperties(fieldProperties);
            formElementList.add(formElement);
        }

        SectionElement sectionElement = new SectionElement();
        sectionElement.setElements(formElementList);
        sectionElement.setClassName("org.joget.apps.form.model.Column");
        ColumnProperties colProperties = new ColumnProperties();
        colProperties.setWidth("100%");
        sectionElement.setProperties(colProperties);

        List<SectionElement> sectionElements = new ArrayList<>();
        sectionElements.add(sectionElement);

        org.joget.marketplace.model.Element e = new org.joget.marketplace.model.Element();
        e.setElements(sectionElements);
        e.setClassName("org.joget.apps.form.model.Section");
        SectionProperties sectionProperties = new SectionProperties();
        sectionProperties.setId("section1");
        sectionProperties.setLabel("section1");
        e.setProperties(sectionProperties);

        List<org.joget.marketplace.model.Element> elementList = new ArrayList<>();
        elementList.add(e);
        jogetForm.setElements(elementList);

        Gson gson = new Gson();
        String formJson = gson.toJson(jogetForm);

        FormDefinition formDefinition = new FormDefinition();
        formDefinition.setJson(formJson);

        formDefinition.setId(jogetForm.getProperties().getId());
        formDefinition.setName(jogetForm.getProperties().getName());
        formDefinition.setTableName(jogetForm.getProperties().getTableName());

        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        Collection<String> errors = appService.createFormDefinition(appDef, formDefinition);

        return formElementList;
    }

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        return super.load(element, primaryKey, formData);
    }

    @Override
    public String getName() {
        return AppPluginUtil.getMessage("formbinder.CrudGenerator.name", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getVersion() {
        final Properties projectProp = new Properties();
        try {
            projectProp.load(this.getClass().getClassLoader().getResourceAsStream("project.properties"));
        } catch (IOException ex) {
            LogUtil.error(getClass().getName(), ex, "Unable to get project version from project properties...");
        }
        return projectProp.getProperty("version");
    }

    @Override
    public String getDescription() {
        return AppPluginUtil.getMessage("formbinder.CrudGenerator.desc", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getLabel() {
        return AppPluginUtil.getMessage("formbinder.CrudGenerator.name", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/CrudGenerator.json", null, true, MESSAGE_PATH);
    }

   public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
       String action = request.getParameter("action");
       if ("testConnection".equals(action)) {
           String message = "";
           Connection conn = null;

           AppDefinition appDef = AppUtil.getCurrentAppDefinition();

           String jdbcDriver = AppUtil.processHashVariable(request.getParameter("jdbcDriver"), null, null, null, appDef);
           String jdbcUrl = AppUtil.processHashVariable(request.getParameter("jdbcUrl"), null, null, null, appDef);
           String jdbcUser = AppUtil.processHashVariable(request.getParameter("jdbcUser"), null, null, null, appDef);
           String jdbcPassword = AppUtil.processHashVariable(request.getParameter("jdbcPassword"), null, null, null, appDef);

           Properties dsProps = new Properties();
           dsProps.put("driverClassName", jdbcDriver);
           dsProps.put("url", jdbcUrl);
           dsProps.put("username", jdbcUser);
           dsProps.put("password", jdbcPassword);

           try ( BasicDataSource ds = BasicDataSourceFactory.createDataSource(dsProps)) {

               conn = ds.getConnection();

               message = AppPluginUtil.getMessage("datalist.jdbcDataListBinder.connectionOk", getClassName(), null);
           } catch (Exception e) {
               LogUtil.error(getClassName(), e, "Test Connection error");
               message = AppPluginUtil.getMessage("datalist.jdbcDataListBinder.connectionFail", getClassName(), null) + "\n" + e.getLocalizedMessage();
           } finally {
               try {
                   if (conn != null && !conn.isClosed()) {
                       conn.close();
                   }
               } catch (SQLException e) {
                   LogUtil.error(DynamicDataSourceManager.class.getName(), e, "");
               }
           }
           try {
               JSONObject jsonObject = new JSONObject();
               jsonObject.accumulate("message", message);
               jsonObject.write(response.getWriter());
           } catch (IOException | JSONException e) {
               //ignore
           }
       } else {
           response.setStatus(HttpServletResponse.SC_NO_CONTENT);
       }
   }

}
