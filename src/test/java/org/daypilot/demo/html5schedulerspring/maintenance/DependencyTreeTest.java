package org.daypilot.demo.html5schedulerspring.maintenance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import lombok.SneakyThrows;

class DependencyTreeTest {
	
	public static final String YWORKS_XSD_DIRECTORY_NAME = "yworks.com";
	public static final String XSD_FILENAME_EXTENSION = ".xsd";

	/*
	 * Only execute this manually or with the generated XML files already in place.
	 * Keeping it as a unit test would block the initial build because of missing the generated XML files.
	 * The same logic is implemented in the ResourceSupplier class.
	 */
	// @Test
	void testDOM() throws IOException, URISyntaxException, ParserConfigurationException, SAXException, XPathExpressionException {
		final String resourceNameSuffix = "-dependency-tree.xml";
		final Set<String> projectModuleNames = new HashSet<>();
		final Set<String> targetResourceNames = new HashSet<>();
		final XPath xPath = XPathFactory.newInstance().newXPath();
		final XPathExpression rootNodeExpression = 
		  xPath.compile("//node[not(@id = //edge/@target)]");
		final Enumeration<URL> resources =
		  Thread.currentThread().getContextClassLoader().getResources("");
		while(resources.hasMoreElements()) {
		  final URL url = resources.nextElement();
		  if("file".equals(url.getProtocol())) {
		    Files.walk(Paths.get(url.toURI()))
			  .filter(path -> path.getName(path.getNameCount() - 1).toString().endsWith(resourceNameSuffix))
			  .forEach(path ->
		    	parse(projectModuleNames, targetResourceNames, xPath, rootNodeExpression, () -> inputStream(path)));
		  } else
		  if("jar".equals(url.getProtocol())) {
			final JarURLConnection connection = (JarURLConnection) url.openConnection();
			final JarFile jarFile = connection.getJarFile();
			final Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
			  final JarEntry entry = entries.nextElement();
			  if (entry.getName().endsWith(resourceNameSuffix)) {
		    	parse(projectModuleNames, targetResourceNames, xPath, rootNodeExpression, () -> inputStream(jarFile, entry));
			  }
			}
		  }
		}
		targetResourceNames.removeAll(projectModuleNames);
		targetResourceNames.stream().sorted().forEach(System.out::println);
		assertFalse(
			targetResourceNames.isEmpty(),
			String.format("No resources found with name ending with '%s'", resourceNameSuffix));
	}

	@SneakyThrows
	private InputStream inputStream(final Path path) {
		return new BufferedInputStream(
			Files.newInputStream(path));
	}

	@SneakyThrows
	private InputStream inputStream(final JarFile jarFile, final JarEntry entry) {
		return new BufferedInputStream(
			jarFile.getInputStream(entry));
	}

	@SneakyThrows
	private void parse(
		final Set<String> projectModuleNames, 
		final Set<String> targetResourceNames, 
		final XPath xPath,
		final XPathExpression rootNodeExpression, 
		final Supplier<InputStream> inputStreamSupplier
	) {
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(false);
		final DocumentBuilder builder = factory.newDocumentBuilder();
		final AtomicReference<Document> doc = new AtomicReference<>();
    	try(final InputStream is = inputStreamSupplier.get()) {
    		doc.set(builder.parse(is));
    	} catch (final Exception e) {
    		e.printStackTrace();
			fail();
		}
		doc.get().getDocumentElement().normalize();
		final Node rootNode = (Node) rootNodeExpression.evaluate(doc.get().getDocumentElement(), XPathConstants.NODE);
		projectModuleNames.add(
			rootNode.getFirstChild().getFirstChild().getFirstChild().getTextContent() + ":compile");
		final XPathExpression expression = xPath.compile(String.format(
			"//node[@id = //edge[@source = '%s']/@target]/*/*/*/text()", 
			rootNode.getAttributes().getNamedItem("id").getTextContent()));
		final NodeList dependencyNodes = (NodeList) expression.evaluate(doc.get().getDocumentElement(), XPathConstants.NODESET);
		for (int n = dependencyNodes.getLength() - 1; n >= 0; n--) {
			final Node child = dependencyNodes.item(n);
			if (child.getNodeType() == Node.TEXT_NODE) {
				targetResourceNames.add(child.getNodeValue().trim());
			}
		}
	}

	/*
	 * This test is good to have because it guarantees the generated XML files can be queried later.
	 */
	@Test
	void validate() throws XPathExpressionException, IOException, URISyntaxException, SAXException {
		final String resourceNameSuffix = "-dependency-tree.xml";
		final List<Path> resourcePaths = new LinkedList<>();
		final List<Source> xsdSources = new LinkedList<>();
		final Enumeration<URL> resources =
		  Thread.currentThread().getContextClassLoader().getResources("");
		while(resources.hasMoreElements()) {
		  final URL url = resources.nextElement();
		  if("file".equals(url.getProtocol())) {
			final Path dir = Paths.get(url.toURI());
			Files.walk(dir).filter(path -> path.getNameCount() > 1).filter(path -> {
			  final int nameCount = path.getNameCount();
			  final String dirName = path.getName(nameCount - 2).toString();
			  final String name = path.getName(nameCount - 1).toString();
			  return dirName.equals(YWORKS_XSD_DIRECTORY_NAME) && name.endsWith(XSD_FILENAME_EXTENSION)
				? addXsdSource(xsdSources, path)
				: name.endsWith(resourceNameSuffix);
			}).forEach(resourcePaths::add);
		  } else
		  if("jar".equals(url.getProtocol())) {
			final JarURLConnection connection = (JarURLConnection) url.openConnection();
			final JarFile jarFile = connection.getJarFile();
			final Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
			  final JarEntry entry = entries.nextElement();
			  final String name = entry.getName();
			  final Path path = Paths.get(name);
			  final int nameCount = path.getNameCount();
			  if (nameCount > 1 && path.getName(nameCount - 2).toString().equals(YWORKS_XSD_DIRECTORY_NAME) && name.endsWith(XSD_FILENAME_EXTENSION)) {
		    	addXsdSource(xsdSources, url, name);
			  }
			}
		  }
		}
		
	    final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
	    final Schema schema = factory.newSchema(xsdSources.toArray(new Source[xsdSources.size()]));
	    final Validator validator = schema.newValidator();

		resourcePaths.stream().forEach(path -> {
	    	try(final InputStream is = inputStream(path)) {
	    		validator.validate(new StreamSource(is));
	    	} catch (final Exception e) {
	    		e.printStackTrace();
				fail();
			}
		});
	}

	private boolean addXsdSource(final List<Source> xsdSources, final Path path) {
		try {
			xsdSources.add(new StreamSource(path.toUri().toURL().toExternalForm()));
		} catch (final Exception e) {
			e.printStackTrace();
			fail();
		}
		return false;
	}

	@SuppressWarnings("deprecation")
	private boolean addXsdSource(final List<Source> xsdSources, final URL jarUrl, final String jarEntryName) {
		try {
			xsdSources.add(new StreamSource( // even deprecated, this is the only way it works
				new URL(jarUrl, urlEncode(Paths.get(jarEntryName), "UTF-8")).toExternalForm()));
		} catch (final Exception e) {
			e.printStackTrace();
			fail();
		}
		return false;
	}

	private String urlEncode(final Path path, final String encodingName) throws UnsupportedEncodingException {
		final int nameCount = path.getNameCount();
		final String[] urlEncodedNames = new String[nameCount];
		for(int i = 0; i < nameCount; i++) {
			urlEncodedNames[i] = URLEncoder.encode(path.getName(i).toString(), encodingName);
		}
		return Paths.get("", urlEncodedNames).toString();
	}
}
