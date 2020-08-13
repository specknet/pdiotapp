package com.specknet.pdiotapp.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.location.places.Place
import com.specknet.pdiotapp.R

/**
 * A placeholder fragment containing a simple view.
 */
class PlaceholderFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel
    lateinit var img: ImageView
    lateinit var description: TextView

    var bgs = arrayOf(R.drawable.ic_bluetooth_icon, R.drawable.ic_recording_icon, R.drawable.ic_live_data_icon)
    var texts = arrayOf(R.string.section_bluetooth, R.string.section_record, R.string.section_live)
    var titles = arrayOf(R.string.section_bluetooth_title, R.string.section_record_title, R.string.section_live_title)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_on_boarding, container, false)
        val textView: TextView = root.findViewById(R.id.section_label)
        textView.text = getString(titles[arguments?.getInt(ARG_SECTION_NUMBER)?.minus(1)!!])

        img = root.findViewById(R.id.section_img)
        img.setBackgroundResource(bgs[arguments?.getInt(ARG_SECTION_NUMBER)?.minus(1)!!])

        description = root.findViewById(R.id.description_text)
        description.text = getString(texts[arguments?.getInt(ARG_SECTION_NUMBER)?.minus(1)!!])
        return root
    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        @JvmStatic
        fun newInstance(sectionNumber: Int): PlaceholderFragment {
            return PlaceholderFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }
}