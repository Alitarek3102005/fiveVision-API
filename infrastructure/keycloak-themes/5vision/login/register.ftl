<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Join - 5Vision</title>
    <link rel="stylesheet" href="${url.resourcesPath}/css/5vision-auth.css">
</head>
<body>
    <div class="vision-container">
        <div class="brand-header">
            <span class="brand-icon">🎬</span>
            <div class="brand-text">Join 5Vision</div>
        </div>

        <#if message?has_content>
            <div class="error-message">
                ${kcSanitize(message.summary)?no_esc}
            </div>
        </#if>

        <form id="kc-register-form" action="${url.registrationAction}" method="post">

            <div style="display: flex; gap: 1rem; margin-bottom: 1.5rem;">
                <div class="form-group" style="flex: 1; margin-bottom: 0;">
                    <label for="firstName">First Name</label>
                    <input type="text" id="firstName" name="firstName" value="${(register.formData.firstName!'')}" autocomplete="given-name" />
                </div>
                <div class="form-group" style="flex: 1; margin-bottom: 0;">
                    <label for="lastName">Last Name</label>
                    <input type="text" id="lastName" name="lastName" value="${(register.formData.lastName!'')}" autocomplete="family-name" />
                </div>
            </div>

            <div class="form-group">
                <label for="email">Email</label>
                <input type="email" id="email" name="email" value="${(register.formData.email!'')}" autocomplete="email" />
            </div>

            <div class="form-group">
                <label for="password">Password</label>
                <input type="password" id="password" name="password" autocomplete="new-password" />
            </div>

            <div class="form-group">
                <label for="password-confirm">Confirm Password</label>
                <input type="password" id="password-confirm" name="password-confirm" />
            </div>

            <div class="form-group">
                <input class="btn-primary" type="submit" value="Create Account"/>
            </div>
        </form>

        <div style="text-align: center; margin-top: 1.5rem; font-size: 0.9rem; color: #94a3b8;">
            Already have an account? <a href="${url.loginUrl}" style="font-weight: bold;">Log In</a>
        </div>
    </div>
</body>
</html>