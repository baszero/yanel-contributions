<?xml version="1.0"?>

<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns="http://www.w3.org/1999/xhtml"
  xmlns:xhtml="http://www.w3.org/1999/xhtml"
  xmlns:x="http://www.xopus.com/xmlns/config"
>
<!--
  exclude-result-prefixes="xhtml"
-->

<xsl:param name="yarep.back2realm" select="'BACK2REALM_IS_NULL'"/>

<!--
See http://xopus.com/Documentation/Developer%20Guide%5CHow%20To%5CUpgrade%20from%20Xopus%202%20to%203.new
-->

<xsl:template match="/">
<html>
  <head>
    <title>Starting Xopus ...</title>
    <!--TODO: It seems that Xopus does not load properly if URL contains special "characters"! -->
    <script type="text/javascript" src="{$yarep.back2realm}Xopus-3.2.10/xopus/xopus.js"></script>
<!--
    <script type="text/javascript" src="{$yarep.back2realm}yanel/resource-types/http%3a%2f%2fwww.wyona.org%2fyanel%2fresource%2f1.0%3a%3axopus3/Xopus-3.2.10/xopus/xopus.js"></script>
-->
<!--
    <script type="text/javascript" src="{$yarep.back2realm}yanel/resource-types/http://www.wyona.org/yanel/resource/1.0::xopus3/Xopus-3.2.10/xopus/xopus.js"></script>
-->
  </head>
  <body bgcolor="#ffffff">
    <div xopus="true" autostart="true">
      ... either Xopus startup hasn't finished yet or something else is wrong ... (see htdocs/xslt/load-xopus-xhtml.xsl of Yanel Xopus 3 resource)
      <xml>
        <x:config version="1.0" xmlns:x="http://www.xopus.com/xmlns/config">
<!--
          <x:javascript src="js/save.js"/>
-->
          <x:pipeline xml="sample.xml" xsd="sample.xsd">
            <x:view name="WYSIWYG View">
              <x:transform xsl="sample.xsl"/>
            </x:view>
<!--
            <x:view name="Tags On View">
              <x:transform xsl="tagson.xsl"/>
            </x:view>
-->
            <x:view name="XML View">
              <x:treeTransform/>
            </x:view>
          </x:pipeline>
<!--
          <x:import src="config.xml"/>
-->
        </x:config>
      </xml>
    </div>
  </body>
</html>
</xsl:template>

</xsl:stylesheet>
