# Cross-Domain Authentication with CSRF Protection

This project implements secure cross-domain authentication using cookie-based sessions with CORS support and comprehensive CSRF protection, enabling multiple frontend applications on different domains to authenticate with a centralized backend API.

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Configuration](#configuration)
- [Security Model](#security-model)
- [CSRF Protection](#csrf-protection)
- [API Endpoints](#api-endpoints)
- [Frontend Integration](#frontend-integration)
- [Testing](#testing)
- [Deployment](#deployment)
- [Troubleshooting](#troubleshooting)
- [Production Considerations](#production-considerations)

---

## Overview

### What This Project Provides

This application uses **cookie-based authentication with CORS** to support authentication from multiple different domains, combined with **CSRF token validation** for all authenticated requests.

**Ideal for:**
- Multi-tenant SaaS applications
- White-label applications with partner-branded domains
- Microservices architectures with frontends on different domains
- Third-party integrations and partner applications

**Key Features:**
- Explicit domain allowlist (no wildcards)
- HttpOnly, Secure cookies with SameSite=None
- Centralized CSRF validation for all authenticated requests
- Clear error responses (401 vs 403)
- Production-ready security

---

## Architecture

### High-Level Design

```
┌──────────────────────┐
│ Frontend Domain 1    │──┐
│ (www.myapp.com)      │  │
└──────────────────────┘  │
                          │
┌──────────────────────┐  │     ┌─────────────────────────┐
│ Frontend Domain 2    │  ├────→│ Backend API             │
│ (another-domain.org) │  │     │ (Ktor Application)      │
└──────────────────────┘  │     │                         │
                          │     │ - Session Management    │
┌──────────────────────┐  │     │ - Authentication        │
│ Frontend Domain 3    │──┘     │ - CORS Handler          │
│ (partner.com)        │        │ - CSRF Validation       │
└──────────────────────┘        └─────────────────────────┘
```

**Key Characteristics:**
- Cookies stored for backend domain only (not shared between frontends)
- Each frontend makes cross-origin requests with `credentials: 'include'`
- CORS validates each request against explicit allowlist
- Session cookies use `SameSite=None` for cross-domain usage
- CSRF tokens validated for all authenticated requests

### Authentication Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                  Complete Authentication Flow                    │
└─────────────────────────────────────────────────────────────────┘

1. User visits frontend (www.myapp.com)
         ↓
2. Frontend submits login via AJAX POST
         ↓
3. Backend validates credentials
         ↓
4. Backend generates CSRF token (UUID)
         ↓
5. Backend creates encrypted session with CSRF token
         ↓
6. Backend sets HttpOnly cookie + returns CSRF token in JSON
         ↓
7. Frontend stores CSRF token in memory
         ↓
8. Subsequent requests include:
   - Cookie (automatic via credentials: 'include')
   - X-CSRF-Token header (manual)
         ↓
9. Backend validates in validate block:
   - Has session cookie? → Yes → Continue
   - CSRF token matches? → Yes → Process request
                        → No → 403 Forbidden
```

### Request Flow with CSRF

```
Request arrives at authenticated endpoint
         ↓
Ktor checks for session cookie
         ↓
   ┌─────────────────────────────────────┐
   │ Has valid session cookie?           │
   └─────────────────────────────────────┘
         ↓ YES                    ↓ NO
         ↓                        ↓
validate { session ->        Challenge block
  session.takeIf {           → 401 "Authentication required"
    validCsrfToken(it)   
  }                      
}                        
         ↓
   ┌─────────────────────────────────────┐
   │ CSRF token valid?                   │
   └─────────────────────────────────────┘
         ↓ YES                    ↓ NO (returns null)
         ↓                        ↓
✅ Process request           Challenge block
                             → 403 "Invalid or missing CSRF token"
```

---

## Configuration

### CORS Configuration

Located in `src/main/kotlin/com/vshpynta/Main.kt`:

```kotlin
install(CORS) {
    // HTTP methods GET, POST are allowed by default
    allowMethod(HttpMethod.Put)
    allowMethod(HttpMethod.Delete)

    // Development: Allow localhost with different ports
    allowHost("localhost:4207")
    allowHost("127.0.0.1:9876")

    // Production: Explicitly allowed domains (HTTPS enforced)
    allowHost("www.myapp.com", schemes = listOf("https"))
    allowHost("another-domain.org", schemes = listOf("https"))

    // Required for cookie-based authentication across origins
    allowCredentials = true
}
```

**What This Configuration Does:**

- **Whitelists domains** that can use JavaScript (`fetch`, `axios`, etc.) to call your API
- **Enables cookie transmission** via `allowCredentials = true`
- **Enforces HTTPS** in production via `schemes = listOf("https")`
- **Prevents data theft** by blocking JavaScript from unauthorized domains

**Important Notes:**
- CORS **only applies to JavaScript** - HTML forms can submit to any domain
- CORS **controls reading responses** - doesn't prevent form submissions
- This is why **CSRF tokens are also required** (see Security Model section)
- Each domain explicitly listed (no wildcards for security)

### Cookie Configuration

**Environment-Specific Settings:**

| Environment | useSecureCookie | cookieSameSite | Purpose |
|-------------|----------------|----------------|---------|
| **Local** (`app-local.conf`) | `false` | `Lax` | HTTP allowed for localhost |
| **Base** (`app.conf`) | `true` | `Lax` | Default settings |
| **Production** (`app-prod.conf`) | `true` | `none` | Cross-domain cookies |

**Cookie Properties:**
- **HttpOnly**: Yes (JavaScript cannot access - XSS protection)
- **Secure**: Yes in production (HTTPS only - MITM protection)
- **SameSite**: `None` in production (enables cross-domain), `Lax` in local
- **Domain**: Not set (cookies bound to backend domain only)
- **Path**: `/`
- **MaxAge**: 1 day (86400 seconds)

---

## Security Model

### Protection Layers

| Security Feature | Status | Implementation | Benefit |
|-----------------|--------|----------------|---------|
| **HttpOnly Cookies** | ✅ Always | Session storage | JavaScript cannot access (XSS protection) |
| **Secure Flag** | ✅ Production | Cookie attribute | HTTPS required (MITM protection) |
| **HTTPS Enforcement** | ✅ Production | CORS schemes | Encrypted transport |
| **Explicit Allowlist** | ✅ Always | CORS allowHost | No wildcards, manual approval |
| **Encrypted Sessions** | ✅ Always | Ktor Sessions | AES encryption + HMAC signing |
| **CSRF Tokens** | ✅ Always | Validate block | Protects all authenticated requests |
| **JSON Responses** | ✅ Always | API design | Proper status codes, no redirects |

### Two Separate Security Mechanisms

This application uses **two independent security layers** that protect against different types of attacks:

#### 1. CORS (Cross-Origin Resource Sharing) - Controls JavaScript Access

```kotlin
// Controls which domains can use JavaScript to call the API
allowHost("www.myapp.com", schemes = listOf("https"))
allowHost("another-domain.org", schemes = listOf("https"))
```

**What CORS Protects:**
- ✅ Prevents JavaScript on `evil.com` from calling `fetch("your-api.com/api/users")` and **reading the response**
- ✅ Blocks data theft via malicious JavaScript
- ✅ Whitelists legitimate frontend domains

**What CORS Does NOT Protect:**
- ❌ HTML form submissions (forms bypass CORS entirely)
- ❌ Direct HTTP requests without JavaScript
- ❌ The attack scenario described below

**Important:** CORS is a **browser-enforced** mechanism that only applies to JavaScript. HTML forms can submit to any domain regardless of CORS configuration.

#### 2. CSRF Tokens - Protects Against Forged Requests

**Why CSRF Protection is Essential:**

When using `SameSite=None` for cross-domain authentication, browsers send cookies with **all** cross-site requests, including malicious ones.

**Attack Scenario Without CSRF Protection (CORS Cannot Stop This):**

```html
<!-- Malicious website: evil.com -->
<form action="https://your-api.com/api/users" method="POST">
  <input type="hidden" name="email" value="attacker@evil.com">
  <input type="hidden" name="password" value="hacked">
</form>
<script>
  document.forms[0].submit();  // Auto-submit when user visits
</script>
```

**What happens:**
1. User is logged into your-api.com ✓
2. User visits evil.com
3. Malicious form auto-submits to your-api.com
4. Browser includes session cookie (SameSite=None allows this!)
5. **CORS does not block this** (forms bypass CORS)
6. ❌ Without CSRF protection, request succeeds

**How CSRF Tokens Prevent This:**

CSRF tokens require a **custom HTTP header** (`X-CSRF-Token`) that:
- ✅ HTML forms **cannot set** (only JavaScript can set custom headers)
- ✅ Requires `fetch()` or `XMLHttpRequest` from an allowed domain
- ✅ Attacker cannot obtain token due to Same-Origin Policy

The attacker **cannot obtain the CSRF token** because:
- Token generated server-side (cryptographically random UUID)
- Stored in encrypted session cookie (HttpOnly, server-side)
- Returned only via AJAX response (not visible in HTML)
- Same-Origin Policy prevents cross-site reading
- Token changes with each login session

### Why You Need Both CORS and CSRF

```
┌─────────────────────────────────────────────────────────────┐
│ Attack Vector: Malicious HTML Form                         │
│ <form action="your-api.com/api/users" method="POST">      │
└─────────────────────────────────────────────────────────────┘
         │
         ├─ Cookie sent? YES (SameSite=None)
         ├─ CORS blocks it? NO ❌ (forms bypass CORS)
         └─ CSRF blocks it? YES ✅ (no X-CSRF-Token header)

┌─────────────────────────────────────────────────────────────┐
│ Attack Vector: Malicious JavaScript                        │
│ fetch("your-api.com/api/users", {credentials: "include"})  │
└─────────────────────────────────────────────────────────────┘
         │
         ├─ Cookie sent? YES (credentials: 'include')
         ├─ CORS blocks it? YES ✅ (evil.com not in allowHost)
         └─ Never reaches CSRF check (blocked by browser first)
```

**Summary:**
- **CORS `allowHost`** = Whitelist for JavaScript API access (prevents data theft)
- **CSRF tokens** = Protection against forged requests (prevents unauthorized actions)
- **Both are required** because they defend against different attack vectors

---

## CSRF Protection

### Implementation Overview

CSRF protection is centralized in the session authentication `validate` block, providing automatic validation for **all authenticated requests** (GET, POST, PUT, DELETE, etc.).

**Note:** Unlike traditional CSRF protection that only validates state-changing operations, this implementation protects all HTTP methods for enhanced security with cross-domain cookies.

### 1. Session Data Structure

```kotlin
@Serializable
data class UserSession(
    val userId: Long,
    val csrfToken: String  // ← CSRF token stored with session
) : Principal
```

### 2. Login - Generate CSRF Token

```kotlin
post("/api/login", webResponse {
    sessionOf(dataSource).use { dbSession ->
        val userId = authenticateUser(dbSession, username, password)
        
        if (userId != null) {
            // Generate cryptographically random CSRF token
            val csrfToken = java.util.UUID.randomUUID().toString()
            
            // Store in encrypted session cookie
            call.sessions.set(UserSession(
                userId = userId,
                csrfToken = csrfToken
            ))
            
            // Return token to client
            JsonWebResponse(
                body = mapOf(
                    "success" to true,
                    "message" to "Login successful",
                    "csrfToken" to csrfToken  // ← Client stores in memory
                )
            )
        } else {
            JsonWebResponse(
                body = mapOf("error" to "Invalid credentials"),
                statusCode = 401
            )
        }
    }
})
```

### 3. Session Authentication with CSRF Validation

```kotlin
session<UserSession>(SESSION_AUTH_PROVIDER) {
    // Validate session and CSRF token (centralized)
    // Note: session parameter is always non-null (Ktor guarantees this)
    validate { session ->
        // Returns session if CSRF token valid, null if invalid
        session.takeIf { validCsrfToken(it) }
    }
    
    // Challenge block: distinguish between failure types
    challenge {
        // IMPORTANT: call.principal<UserSession>() is null here
        // because validate() returned null, so access session directly
        val session = call.sessions.get<UserSession>()
        
        if (session != null) {
            // Session cookie exists but CSRF validation failed
            call.respond(
                KtorJsonWebResponse(
                    body = mapOf("error" to "Invalid or missing CSRF token"),
                    status = HttpStatusCode.Forbidden  // 403
                )
            )
        } else {
            // No session cookie at all
            call.respond(
                KtorJsonWebResponse(
                    body = mapOf("error" to "Authentication required", "requiresAuth" to true),
                    status = HttpStatusCode.Unauthorized  // 401
                )
            )
        }
    }
}
```

### 4. CSRF Validation Helper

```kotlin
/**
 * Validates CSRF token for all authenticated requests.
 * 
 * Note: Validates ALL HTTP methods (GET, POST, PUT, DELETE, etc.),
 * providing additional security for cross-domain cookies with SameSite=None.
 */
private fun ApplicationCall.validCsrfToken(session: UserSession): Boolean {
    val providedToken = request.headers["X-CSRF-Token"]
    return providedToken != null && session.csrfToken == providedToken
}
```

### 5. Protected Endpoints

All routes within `authenticate(SESSION_AUTH_PROVIDER)` are **automatically CSRF-protected**:

```kotlin
authenticate(SESSION_AUTH_PROVIDER) {
    // CSRF validation happens automatically in validate block
    // No manual checks needed in individual endpoints
    
    get("/api/secret", webResponseDb(dataSource) { dbSession ->
        val userSession = call.principal<UserSession>()!!
        val user = findUserById(dbSession, userSession.userId)!!
        
        HtmlWebResponse(
            AppLayout("Welcome, ${user.email}").apply {
                pageBody {
                    h1 { +"Hello there, ${user.email}" }
                    p { +"You're logged in." }
                    p { a(href = "/api/logout") { +"Log out" } }
                }
            }
        )
    })
}
```

### Key CSRF Properties

| Property | Implementation | Benefit |
|----------|---------------|---------|
| **Token Uniqueness** | UUID per session | Unguessable |
| **Token Storage** | Encrypted in HttpOnly cookie | Server-side only |
| **Token Transmission** | JSON response | Client stores in memory |
| **Token Validation** | Centralized in validate block | All requests protected |
| **Token Lifetime** | Tied to session (1 day) | Expires automatically |
| **Validation Scope** | All HTTP methods | Enhanced security |

### Error Responses

| Status Code | Meaning | Client Action |
|-------------|---------|---------------|
| **401 Unauthorized** | No session cookie | Redirect to login |
| **403 Forbidden** | Invalid/missing CSRF token | Check X-CSRF-Token header or re-login |

---

## API Endpoints

### Authentication Endpoints

| Endpoint | Method | Request | Success (200) | Failure |
|----------|--------|---------|---------------|---------|
| `/api/login` | POST | `username` & `password` (form) | `{"success": true, "message": "Login successful", "csrfToken": "..."}` | `401 {"error": "Invalid credentials"}` |
| `/api/logout` | GET | - | `{"success": true, "message": "Logged out successfully"}` | - |
| `/api/secret` | GET | Requires auth + CSRF | HTML page | `401` or `403 {"error": "..."}` |

### Login Endpoint Details

**Request:**
```http
POST /api/login
Content-Type: application/x-www-form-urlencoded

username=user@example.com&password=password
```

**Success Response (200):**
```json
{
  "success": true,
  "message": "Login successful",
  "csrfToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

**Failure Response (401):**
```json
{
  "error": "Invalid credentials"
}
```

**Headers Set:**
```http
Set-Cookie: user-session=<encrypted-value>; Max-Age=86400; Path=/; HttpOnly; Secure; SameSite=None
```

### Logout Endpoint Details

**Request:**
```http
GET /api/logout
```

**Response (200):**
```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

Session cookie is cleared automatically.

### Protected Endpoints

All routes wrapped in `authenticate(SESSION_AUTH_PROVIDER)` require:
1. Valid session cookie
2. Valid CSRF token in `X-CSRF-Token` header

**Missing/invalid authentication triggers challenge block:**
- No session → 401 "Authentication required"
- Invalid CSRF → 403 "Invalid or missing CSRF token"

---

## Frontend Integration

### Requirements

Frontend applications **must**:
1. Use HTTPS in production
2. Include `credentials: 'include'` in all fetch requests
3. Store CSRF token in JavaScript memory (not localStorage)
4. Include `X-CSRF-Token` header in all authenticated requests
5. Handle 401 and 403 responses appropriately

### Complete Authentication Flow

```javascript
// Global CSRF token storage (in memory only!)
let csrfToken = null;

// 1. Login
async function login(username, password) {
    const response = await fetch('https://api-service.com/api/login', {
        method: 'POST',
        credentials: 'include',  // ← CRITICAL for cross-domain cookies
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: new URLSearchParams({ username, password })
    });
    
    const data = await response.json();
    
    if (response.ok) {
        // Store CSRF token in memory
        csrfToken = data.csrfToken;
        console.log('Login successful');
        return true;
    } else {
        console.error(data.error);
        return false;
    }
}

// 2. Access protected resources
async function fetchUserData() {
    if (!csrfToken) {
        console.error('No CSRF token. Please login first.');
        window.location.href = '/login';
        return null;
    }
    
    const response = await fetch('https://api-service.com/api/secret', {
        credentials: 'include',  // Include session cookie
        headers: {
            'X-CSRF-Token': csrfToken  // Include CSRF token
        }
    });
    
    if (response.status === 401) {
        // Not authenticated - redirect to login
        window.location.href = '/login';
        return null;
    } else if (response.status === 403) {
        // CSRF validation failed
        console.error('CSRF token invalid or missing');
        window.location.href = '/login';
        return null;
    }
    
    return await response.text();  // HTML content
}

// 3. Logout
async function logout() {
    const response = await fetch('https://api-service.com/api/logout', {
        credentials: 'include'
    });
    
    const data = await response.json();
    console.log(data.message);
    
    // Clear CSRF token
    csrfToken = null;
    
    // Redirect to home or login
    window.location.href = '/';
}
```

### Global Fetch Wrapper (Recommended)

```javascript
/**
 * Wrapper for all authenticated requests
 * Automatically includes credentials and CSRF token
 */
async function authenticatedFetch(url, options = {}) {
    // Check if CSRF token exists
    if (!csrfToken) {
        console.error('No CSRF token available');
        window.location.href = '/login';
        return null;
    }
    
    // Make request with credentials and CSRF token
    const response = await fetch(url, {
        ...options,
        credentials: 'include',  // Always include
        headers: {
            ...options.headers,
            'X-CSRF-Token': csrfToken  // Always include
        }
    });
    
    // Handle authentication failures
    if (response.status === 401) {
        console.error('Authentication required');
        window.location.href = '/login';
        return null;
    } else if (response.status === 403) {
        console.error('CSRF validation failed');
        window.location.href = '/login';
        return null;
    }
    
    return response;
}

// Usage example
const response = await authenticatedFetch('https://api-service.com/api/users');
const users = await response?.json();
```

### Important Security Notes

**✅ DO:**
- Store CSRF token in JavaScript memory (`let csrfToken = null`)
- Include `credentials: 'include'` on all requests
- Include `X-CSRF-Token` header on all authenticated requests
- Clear token on logout
- Handle 401 and 403 errors appropriately

**❌ DON'T:**
- Store CSRF token in localStorage (vulnerable to XSS)
- Skip `credentials: 'include'` (cookies won't be sent)
- Skip CSRF token header (request will fail with 403)
- Include token in query parameters (logs expose token)
- Share token between browser tabs (each login gets new token)

```javascript
// ❌ BAD: Never do this!
localStorage.setItem('csrfToken', token);

// ✅ GOOD: Store in memory
let csrfToken = null;
```

---

## Testing

### Test CORS Preflight

```bash
curl -H "Origin: https://yourdomain.com" \
     -H "Access-Control-Request-Method: POST" \
     -H "Access-Control-Request-Headers: Content-Type" \
     -X OPTIONS \
     https://your-api.com/api/login -v

# Expected response headers:
# Access-Control-Allow-Origin: https://yourdomain.com
# Access-Control-Allow-Credentials: true
# Access-Control-Allow-Methods: POST
```

### Test Complete Authentication Flow

**1. Login and capture CSRF token:**
```bash
curl -X POST http://localhost:4207/api/login \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -d "username=test@example.com&password=password" \
     -c cookies.txt -v

# Expected response:
# HTTP/1.1 200 OK
# Set-Cookie: user-session=...; Max-Age=86400; Path=/; HttpOnly; Secure; SameSite=None
# {"success":true,"message":"Login successful","csrfToken":"abc-123-xyz"}
```

**2. Access protected endpoint with valid CSRF token (should succeed):**
```bash
curl http://localhost:4207/api/secret \
     -H "X-CSRF-Token: abc-123-xyz" \
     -b cookies.txt

# Expected: HTTP 200 OK
# <html>...Welcome page...</html>
```

**3. Access without CSRF token (should fail with 403):**
```bash
curl http://localhost:4207/api/secret \
     -b cookies.txt

# Expected: HTTP 403 Forbidden
# {"error":"Invalid or missing CSRF token"}
```

**4. Access with invalid CSRF token (should fail with 403):**
```bash
curl http://localhost:4207/api/secret \
     -H "X-CSRF-Token: wrong-token" \
     -b cookies.txt

# Expected: HTTP 403 Forbidden
# {"error":"Invalid or missing CSRF token"}
```

**5. Access without session cookie (should fail with 401):**
```bash
curl http://localhost:4207/api/secret \
     -H "X-CSRF-Token: abc-123-xyz"

# Expected: HTTP 401 Unauthorized
# {"error":"Authentication required","requiresAuth":true}
```

**6. Logout:**
```bash
curl http://localhost:4207/api/logout \
     -b cookies.txt

# Expected: HTTP 200 OK
# {"success":true,"message":"Logged out successfully"}
```

### Verify Cookie in Browser

1. Open browser DevTools (F12)
2. Navigate to Application → Cookies → `https://your-api.com`
3. Verify cookie properties:

```
Name:      user-session
Value:     <encrypted-value>
Domain:    your-api.com
Path:      /
Secure:    ✓ (checkmark)
HttpOnly:  ✓ (checkmark)
SameSite:  None
Max-Age:   86400
```

---

## Deployment

### Environment Configuration

Set environment variable to select configuration:
```bash
export WEB_PLAYGROUND_ENV=prod
```

**Configuration files:**
- `app.conf` - Base configuration
- `app-local.conf` - Local development overrides
- `app-prod.conf` - Production settings

### Build and Run

```bash
# Build application
./gradlew build

# Run locally
./gradlew run

# Production deployment
./build/install/web-playground/bin/web-playground
```

### Adding New Domains

To allow a new domain to authenticate:

1. **Update CORS configuration** in `src/main/kotlin/com/vshpynta/Main.kt`:
   ```kotlin
   install(CORS) {
       // ...existing configuration...
       allowHost("newclient.com", schemes = listOf("https"))
   }
   ```

2. **Rebuild and deploy:**
   ```bash
   ./gradlew build
   ```

3. **Verify CORS works:**
   ```bash
   curl -H "Origin: https://newclient.com" \
        -H "Access-Control-Request-Method: POST" \
        -X OPTIONS \
        https://your-api.com/api/login -v
   ```

No changes required to existing frontend applications or other domains.

---

## Troubleshooting

### Cookie Not Being Set

**Symptoms:**
- Login returns success but no cookie in browser DevTools
- Subsequent requests return 401

**Possible causes:**
- Backend not using HTTPS (Secure flag requires HTTPS)
- Frontend missing `credentials: 'include'`
- Domain not in CORS `allowHost()` list
- `allowCredentials = false` in CORS

**Solution:**
1. Verify backend uses HTTPS in production
2. Check fetch requests include `credentials: 'include'`
3. Verify domain is in allowHost list
4. Check CORS preflight response headers

### CORS Policy Errors

**Error message:**
```
Access to fetch at 'https://api.com/api/login' from origin 
'https://app.com' has been blocked by CORS policy
```

**Solution:**
1. Add domain to `allowHost()` in Main.kt
2. Check domain spelling (including www. prefix)
3. Ensure `schemes = listOf("https")` for production
4. Rebuild and redeploy

### Cookie Not Sent with Requests

**Symptoms:**
- Login succeeds, cookie visible in DevTools
- Protected endpoints return 401

**Solution:**
- Add `credentials: 'include'` to all fetch requests
- Verify cookie hasn't expired (check Max-Age)
- Check request goes to same domain as cookie
- Verify browser allows third-party cookies

### CSRF Validation Failing

**Symptoms:**
- Login succeeds
- Protected endpoints return 403

**Solution:**
- Include `X-CSRF-Token` header in all authenticated requests
- Verify token matches the one from login response
- Check token hasn't been cleared from memory
- Ensure token is valid (not expired with session)

### Debug Checklist

```javascript
// Add this to your frontend for debugging
console.log('CSRF Token:', csrfToken);
console.log('Request URL:', url);
console.log('Request Headers:', {
    'X-CSRF-Token': csrfToken,
    'credentials': 'include'
});

// Check response
console.log('Response Status:', response.status);
console.log('Response Body:', await response.json());
```

---

## Production Considerations

### Security Checklist

- [ ] **HTTPS enabled** on backend
- [ ] **HTTPS enabled** on all frontend domains
- [ ] **Domain allowlist updated** with real domains (remove examples)
- [ ] **Test domains removed** from production CORS config
- [ ] **Cookie Secure flag** enabled (useSecureCookie = true)
- [ ] **SameSite=None** in production config
- [ ] **CSRF tokens** working on all authenticated routes
- [ ] **Error handling** implemented in frontend (401/403)
- [ ] **Rate limiting** considered for login endpoint
- [ ] **Monitoring** set up for authentication failures

### Monitoring

**Log authentication events:**

```kotlin
post("/api/login", webResponse {
    if (userId != null) {
        log.info("Successful login: userId=$userId, origin=${call.request.origin.host}")
    } else {
        log.warn("Failed login attempt: origin=${call.request.origin.host}")
    }
})
```

**Monitor for:**
- Unusual number of failed login attempts
- Authentication from unexpected origins
- High rate of 403 errors (potential CSRF attack)
- Session hijacking patterns

### Domain Management

**Maintain documentation for each allowed domain:**
- Domain name
- Owner/organization
- Date added
- Purpose/use case
- Technical contact
- Date last reviewed

**Review quarterly:**
- Remove inactive domains
- Verify contacts are current
- Check for security updates needed

### Performance Optimization

**Session storage:**
- Consider Redis for distributed sessions
- Monitor session storage size
- Implement session cleanup for expired sessions

**CORS:**
- Cache CORS preflight responses
- Minimize domain list size
- Use CDN for static assets

### Architecture Comparison

| Feature | Reverse Proxy | Subdomain | Cross-Domain (This Project) |
|---------|--------------|-----------|-------------------|
| **Architecture** | Same domain | Same parent domain | Any domains |
| **Example** | `app.com/api/` | `api.example.com` | `myapp.com` + `partner.com` |
| **Cookie Domain** | Current | `.example.com` | Backend only |
| **SameSite** | Strict/Lax | Lax | None |
| **CORS Needed** | No | Yes | Yes |
| **CSRF Risk** | Lowest | Low | Higher (mitigated) |
| **Flexibility** | Limited | Moderate | Maximum |
| **Setup** | Nginx/proxy | DNS config | Code config |
| **Use Case** | Single org | Multi-app, same org | Multi-tenant, any org |

---

## Summary

This project implements a secure, production-ready cross-domain authentication system with the following key features:

**✅ Security:**
- HttpOnly, Secure cookies with SameSite=None
- Encrypted session storage with AES + HMAC
- CSRF protection for all authenticated requests
- Explicit domain allowlist (no wildcards)
- HTTPS enforcement in production
- Clear error responses (401 vs 403)

**✅ Functionality:**
- Cookie-based authentication across any domain
- Centralized CSRF validation
- JSON API responses
- Session management with automatic expiry
- Logout functionality

**✅ Developer Experience:**
- Simple frontend integration
- Clear documentation
- Comprehensive testing examples
- Easy to add new domains
- Production-ready configuration

**Your authentication system is secure, scalable, and ready for production deployment!** 🔒✨

