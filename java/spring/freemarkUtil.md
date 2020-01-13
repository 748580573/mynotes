````java
public class FreeMarkerUtil {

    public static final Logger logger= LoggerFactory.getLogger(FreeMarkerUtil.class);

    public static Template getTeplateByStr(String templateName, String templateStr, Configuration config) throws AdminException {
        if(null == config) {
            config=new Configuration();
        }
        config.setOutputEncoding("UTF-8");
        config.setURLEscapingCharset("UTF-8");
        config.setDefaultEncoding("UTF-8");

        Template template=null;
        try {
            BeansWrapper wrapper=BeansWrapper.getDefaultInstance();
            TemplateHashModel staticModels=wrapper.getStaticModels();
            TemplateHashModel fileStatics=(TemplateHashModel)staticModels.get(FreeMarkerFuncs.class.getName());
            config.setSharedVariable("funcs", fileStatics);
            template=new Template(templateName, new StringReader(templateStr), config);
        } catch(Exception e) {
            logger.error(e.getMessage(), e);
            throw new AdminException("创建模版失败");
        }
        return template;
    }

    public static String process(Template template, Map<String, Object> root) throws AdminException {
        StringWriter writer=new StringWriter();
        try {
            template.process(root, writer);
            return writer.toString();
        } catch(Exception e) {
            logger.error(e.getMessage(), e);
            throw new AdminException("渲染模版失败");
        }
    }

    public static String getHtml(String templateStr, Map<String, Object> root) throws AdminException {
        Configuration config=new Configuration();
        Template template=getTeplateByStr("defaultTmpl", templateStr, config);
        return process(template, root);
    }
}
````

