package net.redpipe.engine.template;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.reactivex.Single;
import io.vertx.reactivex.core.file.FileSystem;
import net.redpipe.engine.core.AppGlobals;

public abstract class AbstractTemplate {
	
	public class TemplateVariants {

		public final Map<Variant, String> variants;
		public final String defaultTemplate;

		public TemplateVariants(String defaultTemplate, Map<Variant, String> variants) {
			this.defaultTemplate = defaultTemplate;
			this.variants = variants;
		}

		public String getVariantTemplate(MediaType mediaType) {
			return getVariantTemplate(mediaType, defaultTemplate);
		}

		public String getVariantTemplate(MediaType mediaType, String templateIfVariantNotFound) {
			Variant variant = new Variant(mediaType, (String)null, null);
			if(variants.containsKey(variant))
				return variants.get(variant);
			return templateIfVariantNotFound;
		}
	}

	protected final Map<String, Object> variables;
	protected final String name;

	public AbstractTemplate(String name, Map<String, Object> variables){
		this.name = name;
		this.variables = variables;
	}

	public AbstractTemplate(String name){
		this(name, new HashMap<>());
	}

	public AbstractTemplate(){
		this(getActionName());
	}

	private static String getActionName() {
		ResourceInfo resourceMethod = ResteasyProviderFactory.getContextData(ResourceInfo.class);
		return "templates/"+resourceMethod.getResourceClass().getSimpleName()+"/"+resourceMethod.getResourceMethod().getName();
	}

	public Map<String, Object> getVariables() {
		return variables;
	}
	
	public String getName() {
		return name;
	}
	
	public AbstractTemplate set(String name, Object value){
		variables.put(name, value);
		return this;
	}
	
	protected Single<TemplateVariants> loadVariants() {
		String path = name;
		int lastSlash = path.lastIndexOf('/');
		String templateDir;
		String namePart; 
		if(lastSlash != -1) {
			templateDir = path.substring(0, lastSlash);
			namePart = path.substring(lastSlash+1);
		} else {
			templateDir = ""; // current dir
			namePart = path;
		}
		FileSystem fs = AppGlobals.get().getVertx().fileSystem();
		return fs.rxReadDir(templateDir)
			.map(list -> loadVariants(fs, templateDir, namePart, list));
	}

	private TemplateVariants loadVariants(FileSystem fs, String templateDir, String namePart,
			List<String> list) {
		Map<Variant, String> variants = new HashMap<>();
		String defaultTemplateExtension = "";
		for (String entry : list) {
			// classpath dir entries are expanded into temp folders which we get as absolute paths
			int lastSlash = entry.lastIndexOf('/');
			if(lastSlash != -1)
				entry = entry.substring(lastSlash+1);
			if(!entry.equals(namePart)
					&& entry.startsWith(namePart)) {
				String extensionWithDot = entry.substring(namePart.length());
				if(!extensionWithDot.startsWith("."))
					continue;
				// get rid of the template extension
				int templateExtension = extensionWithDot.indexOf('.', 1);
				if(templateExtension == -1) {
					// we have a single extension, it's probably the default template extension
					defaultTemplateExtension = extensionWithDot;
					continue;
				}
				String mediaExtension = extensionWithDot.substring(1, templateExtension);
				MediaType mediaType = AbstractTemplate.parseMediaType(mediaExtension);
				variants.put(new Variant(mediaType, (String)null, null), templateDir+"/"+entry);
			}
		}
		return new TemplateVariants(templateDir+"/"+namePart+defaultTemplateExtension, variants);
	}

	public static MediaType parseMediaType(String extension) {
		// FIXME: bigger list, and override in config
		if(extension.equalsIgnoreCase("html"))
			return MediaType.TEXT_HTML_TYPE;
		if(extension.equalsIgnoreCase("xml"))
			return MediaType.APPLICATION_XML_TYPE;
		if(extension.equalsIgnoreCase("txt"))
			return MediaType.TEXT_PLAIN_TYPE;
		if(extension.equalsIgnoreCase("json"))
			return MediaType.APPLICATION_JSON_TYPE;
		System.err.println("Unknown extension type: "+extension);
		return MediaType.APPLICATION_OCTET_STREAM_TYPE;
	}

	public static MediaType parseMediaType(String templatePath, String templateExtension) {
		int lastSlash = templatePath.lastIndexOf('/');
		String templateName;
		if(lastSlash != -1)
			templateName = templatePath.substring(lastSlash+1);
		else
			templateName = templatePath;
		if(templateName.endsWith(templateExtension))
			templateName = templateName.substring(0, templateName.length()-templateExtension.length());
		int lastDot = templateName.lastIndexOf('.');
		if(lastDot != -1)
			return parseMediaType(templateName.substring(lastDot+1));
		// no extension
		return MediaType.APPLICATION_OCTET_STREAM_TYPE;
	}
}
