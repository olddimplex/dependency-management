<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
	version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:y="http://graphml.graphdrawing.org/xmlns" 
	xmlns:z="http://www.yworks.com/xml/graphml"
	>
	<xsl:output method="text" encoding="UTF-8"/>

	<xsl:template match="/">
		<xsl:apply-templates select="//y:node[not(@id = //y:edge/@target)]"/>
	</xsl:template>

	<xsl:template match="y:node">
		<xsl:variable name="root-node-id" select="@id"/>
		<xsl:apply-templates select="//y:node[@id = //y:edge[@source = $root-node-id]/@target]/*/*/*"/>
	</xsl:template>

	<xsl:template match="z:NodeLabel">
		<xsl:value-of select="text()"/><xsl:text>&#10;</xsl:text>
	</xsl:template>
</xsl:stylesheet>