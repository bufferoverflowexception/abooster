package com.yy.sdk.abooster

//import android.content.res.loader.ResourcesLoader
//import android.content.res.loader.ResourcesProvider
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nls.example.testlib2.JavaTest
import com.nls.example.testlib2.JavaTest1
import com.nls.example.testlib2.JavaTest2
import com.nls.example.testlib3.JavaTestLib3
import com.nls.example.testlib3.pkg.KotlinTest
import com.yy.sdk.abooster.package1.Test2

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.tv_test).setOnClickListener {
            //Class.forName("com.yy.sdk.abooster.Test")
            //Toast.makeText(this, "d21", Toast.LENGTH_SHORT).show()
            Test2().test(MainActivity@ this)
            startTest()
        }

        findViewById<View>(R.id.tv_load).setOnClickListener {
            loadPathTest()
        }

        Test().test1(2)
        resourceTest()
        codeTest()
        //Test1().test()
    }

    private fun startTest() {
        val intent = Intent(this, TestActivity::class.java)
        startActivity(intent)
    }

    private fun codeTest() {
        JavaTestLib3.test3()
        JavaTest.test()
        JavaTest1.test()
        JavaTest2.test()
        KotlinTest.test()
    }

    private fun resourceTest() {
        findViewById<TextView>(R.id.tv_test).setTextColor(resources.getColor(R.color.test3_200))
    }

    private fun loadPathTest() {
        val resApk = getExternalFilesDir("out.ap_")
        if (!resApk!!.exists()) {
            return
        }
//        val fileDescriptor = ParcelFileDescriptor.open(resApk, ParcelFileDescriptor.MODE_READ_WRITE)
//        val provider = ResourcesProvider.loadFromApk(fileDescriptor)
//        val loader = ResourcesLoader().apply {
//            this.addProvider(provider)
//        }
//        resources.addLoaders(loader)
    }
}