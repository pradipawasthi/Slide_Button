package com.pradip.slidebuttonlibrary

interface ISlideListener {
    fun onSlideStart()
    fun onSlideMove(percent: Float)
    fun onSlideCancel()
    fun onSlideDone()
}
