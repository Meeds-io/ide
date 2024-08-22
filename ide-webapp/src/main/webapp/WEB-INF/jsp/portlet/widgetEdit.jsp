<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<portlet:defineObjects />
<%
  String[] widgetIdValues = (String[]) request.getAttribute("widgetId");
  String widgetId = widgetIdValues[0];
  String[] portletInstanceValues = (String[]) request.getAttribute("portletInstanceId");
  String portletInstanceId = portletInstanceValues[0];
%>
<div class="VuetifyApp">
  <div data-app="true"
    class="v-application transparent v-application--is-ltr theme--light"
    id="WidgetEditor">
    <script type="text/javascript">
      require(['SHARED/WidgetEditor'], app => app.init(<%=widgetId%>, <%=portletInstanceId%>));
    </script>
  </div>
</div>
