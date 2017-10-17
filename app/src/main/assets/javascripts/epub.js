"use strict";
function getScrollHeight() {
    return document.scrollingElement.scrollHeight;
}

function getScreenHeight() {
    return window.innerHeight;
}

function getCurrentScrollHeight() {
    return document.documentElement.scrollTop;
}

function calcPageCount() {
    return Math.round(getScrollHeight() / getScreenHeight())
}

function scrollAbsY(y) {
    window.scrollBy(0, y)
}