package com.example.hackatum

import react.create
import react.dom.client.createRoot
import web.dom.document
import web.dom.ElementId

fun main() {
    val container = document.getElementById("react-app".unsafeCast<ElementId>()) 
        ?: error("Couldn't find container!")
    createRoot(container).render(App.create())
}
