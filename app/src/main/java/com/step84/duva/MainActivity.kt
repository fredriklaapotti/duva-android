package com.step84.duva

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(),
    HomeFragment.OnFragmentInteractionListener,
    ZonesFragment.OnFragmentInteractionListener,
    SettingsFragment.OnFragmentInteractionListener {

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                switchFragment(HomeFragment())
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_zones -> {
                switchFragment(ZonesFragment())
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_settings -> {
                switchFragment(SettingsFragment())
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    private fun switchFragment(f: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.container, f).commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        switchFragment(HomeFragment())

    }

    override fun onFragmentInteraction(uri: Uri) {
    }
}