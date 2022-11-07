package com.nls.example.testlib3.pkg

import android.util.Log
import com.nls.example.testlib3.JavaTestLib3

/**
 * Created by nls on 2022/11/7
 * Description: KotlinTest
 */
class KotlinTest {

    companion object {
        fun test() {
            Log.d("KotlinTest", "KotlinTest1")
            JavaTestLib3.test3()
        }
    }
}