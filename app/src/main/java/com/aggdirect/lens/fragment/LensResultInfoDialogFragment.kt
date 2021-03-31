package com.aggdirect.lens.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aggdirect.lens.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.lens_fragment_result_info_dialog_list_dialog.view.*

// TODO: Customize parameter argument names
const val ARG_ITEM_ARRAY = "item_array"

/**
 *
 * A fragment that shows a list of items as a modal bottom sheet.
 *
 * You can show this modal bottom sheet from your activity like this:
 * <pre>
 *    ResultInfoDialogFragment.newInstance(30).show(supportFragmentManager, "dialog")
 * </pre>
 */
class ResultInfoDialogFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.lens_fragment_result_info_dialog_list_dialog,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.recyclerView.layoutManager =
            LinearLayoutManager(context)
        view.recyclerView.adapter =
            arguments?.getFloatArray(ARG_ITEM_ARRAY)?.let { ItemAdapter(it) }
    }

    private inner class ViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup
    ) : RecyclerView.ViewHolder(
        inflater.inflate(
            R.layout.lens_fragment_result_info_dialog_list_dialog_item,
            parent,
            false
        )
    ) {

        val text: TextView = itemView.findViewById(R.id.text)
    }

    private inner class ItemAdapter(private val mFloatArray: FloatArray) :
        RecyclerView.Adapter<ViewHolder>() {

        val titles = arrayOf(
            "top_left_x: ",
            "top_left_y: ",
            "top_right_x: ",
            "top_right_y: ",
            "bottom_left_x: ",
            "bottom_left_y: ",
            "bottom_right_x: ",
            "bottom_right_y: "
        )

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context), parent)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val text = titles[position] + mFloatArray[position].toString()
            holder.text.text = text
        }

        override fun getItemCount(): Int {
            return if (mFloatArray.size == 8) mFloatArray.size else 0
        }
    }

    companion object {

        fun newInstance(array: FloatArray): ResultInfoDialogFragment =
            ResultInfoDialogFragment().apply {
                arguments = Bundle().apply {
                    putFloatArray(ARG_ITEM_ARRAY, array)
                }
            }

    }
}