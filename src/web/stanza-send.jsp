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
    <style>
        .send-pending-indicator {
            display: none;
            margin-left: 0.5rem;
            align-items: center;
            gap: 0.35rem;
            color: #444;
        }

        .send-pending-indicator.active {
            display: inline-flex;
        }

        .send-pending-spinner {
            width: 0.9rem;
            height: 0.9rem;
            border: 2px solid #cfd5de;
            border-top-color: #3572b0;
            border-radius: 50%;
            animation: send-pending-spin 0.8s linear infinite;
        }

        @keyframes send-pending-spin {
            to {
                transform: rotate(360deg);
            }
        }
    </style>
</head>
<body>

<admin:FlashMessage/>

<form id="stanza-form" method="post">
    <input name="csrf" value="<c:out value="${csrf}"/>" type="hidden">
    <admin:contentBox title="Send XMPP stanza">

        <table cellpadding="3" cellspacing="0" border="0">
        <tbody>
        <tr valign="top">
            <td width="1%" nowrap>
                <label for="connection">
                    Inject via
                </label>
            </td>
            <td width="99%">
                <select id="connection" name="connection" style="width: 100%;">
                    <option value="router" <c:if test="${empty sessionScope.connection or sessionScope.connection eq 'router'}">selected</c:if>>Default packet router</option>
                    <c:forEach var="connection" items="${connections}">
                        <option value="<c:out value='${connection.value}'/>" <c:if test="${sessionScope.connection eq connection.value}">selected</c:if>><c:out value="${connection.label}"/></option>
                    </c:forEach>
                </select>
            </td>
        </tr>
        <tr valign="top">
            <td width="1%" nowrap>
                <label for="template">
                    Template
                </label>
            </td>
            <td width="99%">
                <select id="template" name="template" style="width: 100%;">
                    <option value="">Select a frequently-used stanza template...</option>
                    <option value="presence-available">Presence: available</option>
                    <option value="message-chat">Message: chat</option>
                    <option value="iq-ping">IQ: ping request (XEP-0199)</option>
                    <option value="iq-roster-get">IQ: roster get</option>
                    <option value="iq-disco-info">IQ: service discovery info</option>
                </select>
                <div style="margin-top: 0.4rem;">
                    <input type="submit" name="insertTemplate" value="Insert template">
                </div>
            </td>
        </tr>
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
    <input id="send-button" type="submit" name="send" value="Send">
    <span id="send-pending-indicator" class="send-pending-indicator" aria-live="polite">
        <span class="send-pending-spinner" aria-hidden="true"></span>
        Sending stanza...
    </span>
    <input type="submit" name="cancel" value="<fmt:message key="global.cancel" />">
</form>
<script>
    (function () {
        var form = document.getElementById('stanza-form');
        if (!form) {
            return;
        }

        var lastClickedSubmitName = null;
        var submitButtons = form.querySelectorAll('input[type="submit"]');

        submitButtons.forEach(function (button) {
            button.addEventListener('click', function () {
                lastClickedSubmitName = button.name || null;
            });
        });

        form.addEventListener('submit', function (event) {
            var submitterName = (event.submitter && event.submitter.name) ? event.submitter.name : lastClickedSubmitName;
            if (submitterName !== 'send') {
                return;
            }

            if (form.dataset.pendingSend === 'true') {
                event.preventDefault();
                return;
            }

            form.dataset.pendingSend = 'true';
            submitButtons.forEach(function (button) {
                button.disabled = true;
            });

            var indicator = document.getElementById('send-pending-indicator');
            if (indicator) {
                indicator.classList.add('active');
            }
        });
    })();
</script>
</body>
</html>
