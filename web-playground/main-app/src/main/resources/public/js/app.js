/**
 * Application JavaScript for the SPA
 * Handles login functionality using fetch API with credentials
 * Implements CSRF protection for cross-domain authentication
 */

// CSRF token storage (in memory, not localStorage for security)
let csrfToken = null;

// Constants
const EVENTS = {
    DOM_CONTENT_LOADED: 'DOMContentLoaded',
    SUBMIT: 'submit'
};

const ENDPOINTS = {
    LOGIN: '/api/login'
};

// Wait for DOM to be fully loaded
document.addEventListener(EVENTS.DOM_CONTENT_LOADED, () => {
    initializeLoginForm();
});

/**
 * Initialize login form with submit handler
 */
function initializeLoginForm() {
    const loginForm = document.getElementById('login-form');
    if (!loginForm) {
        return; // Form not present on this page
    }

    loginForm.addEventListener(EVENTS.SUBMIT, handleLoginSubmit);
}

/**
 * Handle login form submission
 * Calls /api/login endpoint with credentials included for CORS
 */
async function handleLoginSubmit(e) {
    e.preventDefault();

    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const messageDiv = document.getElementById('message');

    // Clear previous messages
    messageDiv.textContent = '';

    try {
        // Call login API using fetch with credentials included
        const response = await fetch(ENDPOINTS.LOGIN, {
            credentials: "include", // Include cookies for cross-origin requests
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: new URLSearchParams({
                username: username,
                password: password
            })
        });

        // Parse JSON response
        const data = await response.json();

        if (response.ok) {
            // Login successful - store CSRF token and display success message
            csrfToken = data.csrfToken;  // Store in memory for protected requests

            messageDiv.style.color = 'green';
            messageDiv.textContent = data.message || 'Login successful!';

            // Optional: redirect to a protected page after brief delay
            // setTimeout(() => window.location.href = '/api/secret', 1000);
        } else {
            // Login failed - display error message from server
            messageDiv.style.color = 'red';
            messageDiv.textContent = data.error || 'Login failed. Please check your credentials.';
        }
    } catch (error) {
        messageDiv.style.color = 'red';
        messageDiv.textContent = 'Error: ' + error.message;
    }
}

/**
 * Navigate to protected page with CSRF token
 * The /api/secret endpoint requires both authentication and CSRF token
 *
 * This function demonstrates how to include CSRF token when navigating to protected pages.
 * In a real SPA, you might fetch the page content via AJAX and render it dynamically.
 */
async function navigateToSecretPage() {
    if (!csrfToken) {
        console.error('CSRF token not available. Please login first.');
        window.location.href = '/api/login';
        return;
    }

    // For traditional navigation, CSRF token needs to be included differently
    // Option 1: Use fetch to get the page content
    try {
        const response = await fetch('/api/secret', {
            credentials: 'include',  // Include session cookie
            headers: {
                'X-CSRF-Token': csrfToken  // Include CSRF token
            }
        });

        if (response.ok) {
            // Successfully accessed protected page
            const html = await response.text();
            // In a real SPA, you might update the page content here
            // document.body.innerHTML = html;
            console.log('Protected page accessed successfully');

            // Or simply navigate (token would need to be sent via other means for full page navigation)
            window.location.href = '/api/secret';
        } else if (response.status === 403) {
            // CSRF validation failed
            const result = await response.json();
            console.error('CSRF validation failed:', result.error);
            // Token invalid or expired - redirect to login
            window.location.href = '/api/login';
        } else if (response.status === 401) {
            // Not authenticated
            console.error('Authentication required');
            window.location.href = '/api/login';
        }
    } catch (error) {
        console.error('Error accessing protected page:', error.message);
    }
}

