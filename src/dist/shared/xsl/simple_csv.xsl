<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="text"/>
  <xsl:template match="/">
  	<xsl:text>TITLE,GENRE,YEAR,STATUS,DEVELOPER,PUBLISHER,CUSTOM1,CUSTOM2,CUSTOM3,CUSTOM4,CUSTOM5,CUSTOM6,CUSTOM7,CUSTOM8,CUSTOM9,CUSTOM10,LINK1,LINK2,LINK3,LINK4,LINK5,LINK6,LINK7,LINK8,LINK1TITLE,LINK2TITLE,LINK3TITLE,LINK4TITLE,LINK5TITLE,LINK6TITLE,LINK7TITLE,LINK8TITLE,FAVORITE,CAPTURES,CONFIGFILE,SETUP,SETUPPARAMETERS,ALTEXE1,ALTEXE1PARAMETERS,ALTEXE2,ALTEXE2PARAMETERS,DOSBOX,DOSBOXVERSION</xsl:text>
  	<xsl:text>&#10;</xsl:text>
    <xsl:for-each select="document/profile">
      <xsl:sort select="title"/>
      	<xsl:text>"</xsl:text>
      	<xsl:value-of select="title"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/genre"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/year"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/status"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/developer"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/publisher"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/custom1"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/custom2"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/custom3"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/custom4"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/custom5"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/custom6"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/custom7"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/custom8"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/custom9"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/custom10"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/link1/raw"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/link2/raw"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/link3/raw"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/link4/raw"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/link5/raw"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/link6/raw"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/link7/raw"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/link8/raw"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/link1/title"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/link2/title"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/link3/title"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/link4/title"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/link5/title"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/link6/title"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/link7/title"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/link8/title"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="meta-info/favorite"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="captures/raw"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="config-file/raw"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="setup"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="setup-parameters"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="altexe1"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="altexe1-parameters"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="altexe2"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="altexe2-parameters"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="dosbox/title"/>
      	<xsl:text>","</xsl:text>
      	<xsl:value-of select="dosbox/version"/>
      	<xsl:text>"</xsl:text>
      	<xsl:text>&#10;</xsl:text>
    </xsl:for-each>
  </xsl:template>
</xsl:stylesheet>