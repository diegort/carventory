package es.dmontesinos.android.carventory.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

// Add this class for list spacing
class ListSpacingItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.left = spacing
        outRect.right = spacing
        outRect.top = spacing / 2
        outRect.bottom = spacing / 2
    }
}