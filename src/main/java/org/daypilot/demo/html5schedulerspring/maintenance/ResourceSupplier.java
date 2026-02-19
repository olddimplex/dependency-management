package org.daypilot.demo.html5schedulerspring.maintenance;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import lombok.SneakyThrows;

public class ResourceSupplier implements Supplier<Collection<Resource>> {

	@Override
	@SneakyThrows
	public Collection<Resource> get() {
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
		final AtomicLong id = new AtomicLong();
		return targetResourceNames
			.stream()
			.sorted()
			.map(name -> new Resource(id.getAndIncrement(), name))
			.toList();
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
}
