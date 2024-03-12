package com.pradip.slidebuttonlib

import android.content.res.Resources
import android.util.TypedValue

object Util {
    fun dp2px(dp: Int): Float {
        return Resources.getSystem().displayMetrics.density * dp
    }

    fun sp2px(spVal: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            spVal.toFloat(),
            Resources.getSystem().displayMetrics
        ).toInt()
    }
}
