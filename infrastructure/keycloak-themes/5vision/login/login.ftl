<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Log In - 5Vision</title>
    <link rel="stylesheet" href="${url.resourcesPath}/css/5vision-auth.css">
</head>
<body>
    <div class="vision-container">
        <div class="brand-header">
            <span class="brand-icon">🎬</span>
            <div class="brand-text">5Vision</div>
        </div>

        <#if message?has_content>
            <div class="error-message">
                ${kcSanitize(message.summary)?no_esc}
            </div>
        </#if>

        <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
            <div class="form-group">
                <label for="username">Username or Email</label>
                <input id="username" name="username" value="${(login.username!'')}" type="text" autofocus autocomplete="off" placeholder="Enter your credentials" />
            </div>

            <div class="form-group">
                <label for="password">Password</label>
                <input id="password" name="password" type="password" autocomplete="off" placeholder="Enter your password" />
            </div>

            <div class="auth-links">
                <#if realm.resetPasswordAllowed>
                    <a href="${url.loginResetCredentialsUrl}">Forgot Password?</a>
                </#if>
            </div>

            <div class="form-group">
                <input class="btn-primary" name="login" id="kc-login" type="submit" value="Sign In"/>
            </div>
        </form>

        <#if realm.registrationAllowed>
            <div style="text-align: center; margin-top: 1.5rem; font-size: 0.9rem; color: #94a3b8;">
                New to 5Vision? <a href="${url.registrationUrl}" style="font-weight: bold;">Sign Up</a>
            </div>
        </#if>
    </div>
</body>
</html>