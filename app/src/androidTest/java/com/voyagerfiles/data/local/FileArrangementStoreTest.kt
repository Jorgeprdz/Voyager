package com.voyagerfiles.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.voyagerfiles.data.model.FileArrangement
import com.voyagerfiles.data.model.GroupBy
import com.voyagerfiles.data.model.SortBy
import com.voyagerfiles.data.model.SortOrder
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileArrangementStoreTest {
    @Test fun tabAndSplitPaneSettingsSurviveStoreRecreationIndependently() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val first = "test:tab:main"
        val second = "test:tab:main:dual"
        val third = "test:tab:another"
        val store = FileArrangementStore(context)
        store.write(first, FileArrangement(sortBy = SortBy.SIZE,
            groupBy = GroupBy.TYPE, groupOrder = SortOrder.DESCENDING))
        store.write(second, FileArrangement(sortBy = SortBy.DATE,
            sortOrder = SortOrder.DESCENDING, groupBy = GroupBy.NAME))
        store.write(third, FileArrangement(sortBy = SortBy.TYPE, groupBy = GroupBy.SIZE))

        val reopened = FileArrangementStore(context)
        assertEquals(SortBy.SIZE, reopened.read(first).sortBy)
        assertEquals(GroupBy.TYPE, reopened.read(first).groupBy)
        assertEquals(SortOrder.DESCENDING, reopened.read(first).groupOrder)
        assertEquals(SortBy.DATE, reopened.read(second).sortBy)
        assertEquals(GroupBy.NAME, reopened.read(second).groupBy)
        assertEquals(SortBy.TYPE, reopened.read(third).sortBy)
        assertEquals(GroupBy.SIZE, reopened.read(third).groupBy)
    }
}
