"use strict";
function getScrollHeight() {
    return document.documentElement.scrollHeight;
}

function getScreenHeight() {
    return window.innerHeight;
}

function getCurrentScrollHeight() {
    return document.documentElement.scrollTop;
}

function calcPageCount() {
    return Math.round(getScrollHeight() / getScreenHeight() - 0.5);
}

function scrollAbsY(y) {
    window.scrollTo(0, y);
}