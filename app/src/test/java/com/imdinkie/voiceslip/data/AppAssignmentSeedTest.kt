package com.imdinkie.voiceslip.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppAssignmentSeedTest {
    @Test
    fun unassigningSeededAppCreatesDurableExplicitMarker() {
        val assignment = appAssignmentForCategorySelection(
            packageName = "org.telegram.messenger",
            categoryId = null,
            nowMillis = 42L
        )

        assertEquals("org.telegram.messenger", assignment.packageName)
        assertNull(assignment.categoryId)
        assertTrue(assignment.explicitlyUnassigned)
        assertEquals(42L, assignment.updatedAtMillis)
        assertFalse(shouldApplySeedAssignment(assignment, CATEGORY_PERSONAL))
    }

    @Test
    fun categoryOtherSelectionAlsoBlocksDefaultReseeding() {
        val assignment = appAssignmentForCategorySelection(
            packageName = "org.telegram.messenger",
            categoryId = CATEGORY_OTHER,
            nowMillis = 100L
        )

        assertNull(assignment.categoryId)
        assertTrue(assignment.explicitlyUnassigned)
        assertFalse(shouldApplySeedAssignment(assignment, CATEGORY_PERSONAL))
    }

    @Test
    fun seededAppWithoutAssignmentCanReceiveDefaultCategory() {
        assertTrue(shouldApplySeedAssignment(existingAssignment = null, seedCategoryId = CATEGORY_PERSONAL))
    }

    @Test
    fun explicitCategorySelectionIsNotMarkedUnassigned() {
        val assignment = appAssignmentForCategorySelection(
            packageName = "org.telegram.messenger",
            categoryId = CATEGORY_WORK,
            nowMillis = 200L
        )

        assertEquals(CATEGORY_WORK, assignment.categoryId)
        assertFalse(assignment.explicitlyUnassigned)
        assertFalse(shouldApplySeedAssignment(assignment, CATEGORY_PERSONAL))
    }
}
