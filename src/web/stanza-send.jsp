<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core_1_1" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="admin" prefix="admin" %>
<jsp:useBean scope="request" id="csrf" type="java.lang.String"/>
<!DOCTYPE html>
<html>
<head>
    <title>XML Debugger Stanza Sending Tool</title>
    <meta name="pageID" content="stanza-sender"/>
</head>
<body>

<admin:FlashMessage/>

<form method="post">
    <input name="csrf" value="<c:out value="${csrf}"/>" type="hidden">
    <admin:contentBox title="Send XMPP stanza">

        <table cellpadding="3" cellspacing="0" border="0">
        <tbody>
        <tr valign="top">
            <td width="1%" nowrap>
                <label for="stanza">
                    Stanza to Send
                </label>
            </td>
            <td width="99%">
                <textarea id="stanza" name="stanza" style="width: 100%; height: 20rem;"><c:if test="${not empty sessionScope.stanza}"><c:out value="${sessionScope.stanza}"/></c:if></textarea>
            </td>
        </tr>
        <c:if test="${not empty sessionScope.result}">
            <tr valign="top">
                <td width="1%" nowrap>
                    <label for="result">
                        Result
                    </label>
                </td>
                <td width="99%">
                    <textarea id="result" readonly style="width: 100%; height: 20rem;"><c:out value="${sessionScope.result}"/></textarea>
                </td>
            </tr>
        </c:if>
        </tbody>
        </table>
    </admin:contentBox>
    <input type="submit" name="send" value="Send">
    <input type="submit" name="cancel" value="<fmt:message key="global.cancel" />">
</form>
</body
</html>
