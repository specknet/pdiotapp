package com.specknet.pdiotapp.onboarding

import android.animation.ArgbEvaluator
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.ui.main.SectionsPagerAdapter

class OnBoardingActivity : AppCompatActivity() {

    lateinit var mSectionsPagerAdapter: SectionsPagerAdapter
    private lateinit var mViewPager: ViewPager
    private lateinit var mNextBtn: ImageButton
    private lateinit var mSkipBtn: Button
    private lateinit var mFinishBtn: Button

    private lateinit var zero: ImageView
    private lateinit var one: ImageView
    private lateinit var two: ImageView
    private lateinit var indicators: Array<ImageView>

    private var lastLeftValue = 0

    private lateinit var mCoordinator: CoordinatorLayout
    private val TAG = "PagerActivity"

    private var page = 0 // to track page position


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_on_boarding)

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)

        mNextBtn = findViewById(R.id.intro_btn_next)
        mNextBtn.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.ic_chevron_right)
        )

        mSkipBtn = findViewById(R.id.intro_btn_skip)
        mFinishBtn = findViewById(R.id.intro_btn_finish)

        zero = findViewById(R.id.intro_indicator_0)
        one = findViewById(R.id.intro_indicator_1)
        two = findViewById(R.id.intro_indicator_2)

        mCoordinator = findViewById(R.id.main_content)

        indicators = arrayOf(zero, one, two)

        // Set up the ViewPages with the sections adapter
        mViewPager = findViewById(R.id.container)
        mViewPager.adapter = mSectionsPagerAdapter
        mViewPager.setCurrentItem(page)
        updateIndicators(page)

        val color1 = ContextCompat.getColor(this, R.color.cyan)
        val color2 = ContextCompat.getColor(this, R.color.orange)
        val color3 = ContextCompat.getColor(this, R.color.green)

        val colorList = arrayOf(color1, color2, color3)
        val evaluator = ArgbEvaluator()

        mViewPager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                /*
                color update
                 */
                val colorUpdate = evaluator.evaluate(
                    positionOffset,
                    colorList[position],
                    colorList[if (position === 2) position else position + 1]
                ) as Int
                mViewPager.setBackgroundColor(colorUpdate)
            }

            override fun onPageSelected(position: Int) {
                page = position
                updateIndicators(page)

                when(position) {
                    0 -> mViewPager.setBackgroundColor(color1)
                    1 -> mViewPager.setBackgroundColor(color2)
                    2 -> mViewPager.setBackgroundColor(color3)
                }

                mNextBtn.visibility = if (position === 2) View.GONE else View.VISIBLE
                mFinishBtn.visibility = if (position === 2) View.VISIBLE else View.GONE

            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })

        mNextBtn.setOnClickListener {
            page++
            mViewPager.setCurrentItem(page, true)
        }

        mSkipBtn.setOnClickListener {
            finish()
        }

        mFinishBtn.setOnClickListener {
            finish()
            // TODO only here should you save the shared preference
        }
    }

    fun updateIndicators(position: Int) {
        for(i in indicators.indices) {
            if(i==position) {
                indicators[i].setBackgroundResource(R.drawable.indicator_selected)
            }
            else indicators[i].setBackgroundResource(R.drawable.indicator_unselected)

        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_pager, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_settings) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}