<%@page import="org.exoplatform.portal.application.PortalRequestContext"%>
<%@page import="io.meeds.ide.model.Widget"%>
<%@page import="java.time.ZoneOffset"%>
<%@page import="io.meeds.ide.service.WidgetService"%>
<%@page import="org.exoplatform.container.ExoContainerContext"%>
<%@taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<portlet:defineObjects />
<%
  String[] widgetIdValues = (String[]) request.getAttribute("widgetId");
  String widgetId = widgetIdValues[0];
  Widget widget = ExoContainerContext.getService(WidgetService.class)
      .getWidget(Long.parseLong(widgetId));
  if (widget != null) {
    long widgetLastModified = widget.getModifiedDate()
       .toEpochSecond(ZoneOffset.UTC);
%>
<div class="VuetifyApp">
  <div data-app="true"
    class="v-application transparent v-application--is-ltr theme--light"
    id="WidgetPortlet<%=renderRequest.getWindowID()%>">
    <link href="/ide/rest/widgets/<%=widgetId%>/css?v=<%=widgetLastModified%>"  type="text/css" rel="stylesheet">
    <script src="/ide/rest/widgets/<%=widgetId%>/js?v=<%=widgetLastModified%>" type="text/javascript"></script>
    <div class="application-body full-width">
      <%= widget.getHtml() == null ? "" : widget.getHtml() %>
    </div>
  </div>
</div>
<% } %>