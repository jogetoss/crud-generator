package org.joget.marketplace;

import com.google.gson.Gson;
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
import javax.sql.DataSource;
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
import org.joget.marketplace.model.Properties;
import org.joget.marketplace.model.FieldProperties;
import org.joget.marketplace.model.LoadBinderProperties;
import org.joget.marketplace.model.MetaData;
import org.joget.marketplace.model.SectionElement;
import org.joget.marketplace.model.SectionProperties;
import org.joget.marketplace.model.StoreBinder;
import org.joget.marketplace.model.StoreBinderProperties;
import org.joget.marketplace.model.Validator;
import org.joget.marketplace.model.ValidatorProperties;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

public class CrudGenerator extends WorkflowFormBinder {

    @Override
    public FormRowSet store(Element element, FormRowSet rows, FormData formData) {
        String tableName = formData.getRequestParameter("table_name");
        List<MetaData> metaDataList = new ArrayList<>();
        Connection con = null;
        try {
            DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
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
            List<FormElement> formElementList = generateForm(tableName, metaDataList);
            //generateDataList(tableName, formElementList);
            generateCrud(tableName, formElementList);
        } catch (SQLException | BeansException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        } finally {
            //always close the connection after used
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {/* ignored */
            }
        }

        FormRowSet frs = super.store(element, rows, formData);
        return frs;
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

    public List<FormElement> generateForm(String tableName, List<MetaData> metaDataList) {

        // do the custom stuff 
        JogetForm jogetForm = new JogetForm();
        jogetForm.setClassName("org.joget.apps.form.model.Form");
        Properties props = new Properties();
        props.setId(tableName);
        props.setName(tableName.toUpperCase());
        props.setTableName(tableName);

        LoadBinder lb = new LoadBinder();
        lb.setClassName("org.joget.plugin.enterprise.DatabaseWizardLoadBinder");
        LoadBinderProperties lbProperties = new LoadBinderProperties();
        lbProperties.setJdbcDatasource("default");
        lbProperties.setAutoHandleWorkflowVariable("true");
        lbProperties.setAutoHandleFiles("");
        lbProperties.setTableName(tableName);
        lbProperties.setKeyColumn("id");
        lbProperties.setExtraCondition("");
        lb.setProperties(lbProperties);
        props.setLoadBinder(lb);

        StoreBinder sb = new StoreBinder();
        sb.setClassName("org.joget.plugin.enterprise.DatabaseWizardStoreBinder");
        StoreBinderProperties sbProperties = new StoreBinderProperties();
        sbProperties.setJdbcDatasource("default");
        sbProperties.setAutoHandleWorkflowVariable("true");
        sbProperties.setAutoHandleFiles("");
        sbProperties.setTableName(tableName);
        sbProperties.setKeyColumn("id");
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
        return "CRUD Generator Form Binder";
    }

    @Override
    public String getVersion() {
        return "8.0.0";
    }

    @Override
    public String getDescription() {
        return "CRUD Generator Form Binder";
    }

    @Override
    public String getLabel() {
        return "CRUD Generator Form Binder";
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }

}
