package com.vshpynta.web.html

import io.ktor.server.html.Placeholder
import io.ktor.server.html.Template
import io.ktor.server.html.insert
import kotlinx.html.BODY
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.styleLink
import kotlinx.html.title

/**
 * Main HTML layout template for the application.
 *
 * This template is intended to be used as a shared wrapper around
 * page-specific content, so that all pages get a consistent `<head>`
 * section (title, CSS, etc.) and basic `<body>` structure.
 *
 * Typical usage from a route:
 *
 * ```kotlin
 * call.respondHtml {
 *     insert(AppLayout(pageTitle = "Home")) {
 *         pageBody {
 *             h1 { +"Welcome" }
 *             p { +"This is the home page" }
 *         }
 *     }
 * }
 * ```
 *
 * @property pageTitle Optional per-page title. When provided, it is
 *   rendered as `<pageTitle> - KotlinWebPlayground`; otherwise just
 *   `KotlinWebPlayground` is used as the document title.
 */
class AppLayout(
    val pageTitle: String? = null
) : Template<HTML> {

    /**
     * Placeholder for page-specific `<body>` content.
     *
     * Callers use this placeholder to inject their own HTML into the
     * layout's `<body>` tag while keeping the shared head/structure.
     */
    val pageBody = Placeholder<BODY>()

    /**
     * Applies the layout to the given [HTML] builder.
     *
     * - Computes the final `<title>` based on [pageTitle].
     * - Adds a link to the main stylesheet `/css/app.css`.
     * - Renders the [pageBody] placeholder inside `<body>`.
     */
    override fun HTML.apply() {
        val pageTitlePrefix = pageTitle?.let { "$it - " } ?: ""

        head {
            title {
                +"${pageTitlePrefix}KotlinWebPlayground"
            }
            styleLink("/css/app.css")
        }
        body {
            insert(pageBody)
        }
    }
}
