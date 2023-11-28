<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core_1_1" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="admin" prefix="admin" %>
<jsp:useBean scope="request" id="errorMessage" class="java.lang.String"/>
<jsp:useBean scope="request" id="warningMessage" class="java.lang.String"/>
<jsp:useBean scope="request" id="successMessage" class="java.lang.String"/>
<jsp:useBean scope="request" id="c2s" type="java.lang.Boolean"/>
<jsp:useBean scope="request" id="ssl" type="java.lang.Boolean"/>
<jsp:useBean scope="request" id="extcomp" type="java.lang.Boolean"/>
<jsp:useBean scope="request" id="cm" type="java.lang.Boolean"/>
<jsp:useBean scope="request" id="interpreted" type="java.lang.Boolean"/>
<jsp:useBean scope="request" id="logWhitespace" type="java.lang.Boolean"/>
<jsp:useBean scope="request" id="loggingToStdOut" type="java.lang.Boolean"/>
<jsp:useBean scope="request" id="loggingToFile" type="java.lang.Boolean"/>
<jsp:useBean scope="request" id="csrf" type="java.lang.String"/>
<!DOCTYPE html>
<html>
<head>
    <title>XML Debugger Properties</title>
    <meta name="pageID" content="debugger-conf"/>
</head>
<body>

<admin:FlashMessage/>

<form method="post">
    <input name="csrf" value="<c:out value="${csrf}"/>" type="hidden">
    <div class="jive-contentBoxHeader">
        Debug connections
    </div>
    <div class="jive-contentBox">
        <table cellpadding="3" cellspacing="0" border="0">
        <tbody>
        <tr valign="middle">
            <td width="1%" nowrap>
                <input id="rb01" type="checkbox" name="c2s" <c:if test="${c2s}">checked</c:if>/>
            </td>
            <td width="99%">
                <label for="rb01">
                    Client (default port)
                </label>
            </td>
        </tr>
        <tr valign="middle">
            <td width="1%" nowrap>
                <input id="rb02" type="checkbox" name="ssl" <c:if test="${ssl}">checked</c:if>/>
            </td>
            <td width="99%">
                <label for="rb02">
                    Client (Direct TLS port)
                </label>
            </td>
        </tr>
        <tr valign="middle">
            <td width="1%" nowrap>
                <input id="rb03" type="checkbox" name="extcomp" <c:if test="${extcomp}">checked</c:if>/>
            </td>
            <td width="99%">
                <label for="rb03">
                    External Component
                </label>
            </td>
        </tr>
        <tr valign="middle">
            <td width="1%" nowrap>
                <input id="rb04" type="checkbox" name="cm" <c:if test="${cm}">checked</c:if>/>
            </td>
            <td width="99%">
                <label for="rb04">
                    Connection Manager
                </label>
            </td>
        </tr>
        <tr valign="middle">
            <td width="1%" nowrap>
                <input id="rb08" type="checkbox" name="logWhitespace" <c:if test="${logWhitespace}">checked</c:if>/>
            </td>
            <td width="99%">
                <label for="rb08">
                    Log whitespace only traffic
                </label>
            </td>
        </tr>
        <tr valign="middle">
            <td width="1%" nowrap>
                <input id="rb05" type="checkbox" name="interpreted" <c:if test="${interpreted}">checked</c:if>/>
            </td>
            <td width="99%">
                <label for="rb05">
                    Interpreted XML
                </label>
            </td>
        </tr>
        <tr valign="middle">
            <td width="1%" nowrap>
                <input id="rb06" type="checkbox" name="loggingToStdOut" <c:if test="${loggingToStdOut}">checked</c:if>/>
            </td>
            <td width="99%">
                <label for="rb06">
                    Log to STDOUT
                </label>
            </td>
        </tr>
        <tr valign="middle">
            <td width="1%" nowrap>
                <input id="rb07" type="checkbox" name="loggingToFile" <c:if test="${loggingToFile}">checked</c:if>/>
            </td>
            <td width="99%">
                <label for="rb07">
                    Log to file
                </label>
            </td>
        </tr>
        </tbody>
        </table>
    </div>
    <input type="submit" name="update" value="<fmt:message key="global.save_settings" />">
    <input type="submit" name="cancel" value="<fmt:message key="global.cancel" />">
</form>
</body
</html>
