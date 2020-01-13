package com.android.kubota.ui

import android.animation.*
import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.*
import android.widget.ImageView
import androidx.annotation.CallSuper
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.android.kubota.R
import com.android.kubota.utility.AnimationUtils
import com.google.android.material.animation.ChildrenAlphaProperty
import com.google.android.material.animation.MotionSpec
import com.google.android.material.animation.MotionTiming
import com.google.android.material.animation.Positioning
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.transformation.ExpandableTransformationBehavior
import com.google.android.material.transformation.FabTransformationScrimBehavior
import com.google.android.material.transformation.TransformationChildCard
import com.google.android.material.transformation.TransformationChildLayout
import java.util.ArrayList
import java.util.HashMap


class CustomFabTransformationBehavior: ExpandableTransformationBehavior {
    private var importantForAccessibilityMap: MutableMap<View, Int>? = null
    private val tmpRect = Rect()
    private val tmpRectF1 = RectF()
    private val tmpRectF2 = RectF()
    private val tmpArray = IntArray(2)

    constructor() {}

    constructor(context:Context, attrs:AttributeSet) : super(context, attrs) {}

    @CallSuper
    override fun onExpandedStateChange(dependency: View, child: View, expanded: Boolean, animated: Boolean): Boolean {
        updateImportantForAccessibility(child, expanded)
        return super.onExpandedStateChange(dependency, child, expanded, animated)
    }

    @CallSuper
    override fun layoutDependsOn(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
        check(child.visibility != View.GONE) { "This behavior cannot be attached to a GONE view. Set the view to INVISIBLE instead." }

        if (dependency is FloatingActionButton) {
            dependency.expandedComponentIdHint.let {
                return it == 0 || it == child.id
            }
        }

        return false
    }

    @CallSuper
    override fun onAttachedToLayoutParams(lp: CoordinatorLayout.LayoutParams) {
        if (lp.dodgeInsetEdges == 0) {
            lp.dodgeInsetEdges = 80
        }
    }

    override fun onCreateExpandedStateChangeAnimation(dependency:View, child:View, expanded:Boolean, isAnimating:Boolean): AnimatorSet {
        val spec = onCreateMotionSpec(dependency, expanded)
        val animations = ArrayList<Animator>()
        val listeners = ArrayList<Animator.AnimatorListener>()

        /*
        Animation Order:
        1. Rotate Fab
        2. Translate each row up from the top of the fab
        3. Fade each row
        */
        createRotationAnimation(dependency, child, expanded, spec, animations)
        createChildrenFadeAnimation(child, expanded, isAnimating, spec, animations)
        val childBounds = tmpRectF1
        createTranslationYAnimation(dependency, child, expanded, isAnimating, spec, animations, childBounds)

        val set = AnimatorSet()
        playTogether(set, animations)
        set.addListener(object:AnimatorListenerAdapter() {
            override fun onAnimationStart(animation:Animator) {
                if (expanded) {
                    child.visibility = View.VISIBLE
                }

            }

            override fun onAnimationEnd(animation:Animator) {
                if (!expanded) {
                    child.visibility = View.INVISIBLE
                }
            }
        })

        val count = listeners.size
        for (i in 0 until count) {
            set.addListener(listeners[i])
        }

        return set
    }

    private fun onCreateMotionSpec(dependency: View, expanded: Boolean): FabTransformationSpec {
        val specRes = if(expanded) R.animator.fab_transformation_show else R.animator.fab_transformation_hide

        MotionSpec.createFromResource(dependency.context, specRes)?.let {
            return FabTransformationSpec(it, Positioning(Gravity.TOP, 0.0f, 0.0f))
        }

        throw IllegalArgumentException("Can't load animation resource ID #0x${Integer.toHexString(specRes)}")
    }

    private fun createTranslationYAnimation(dependency: View, child: View, expanded: Boolean, currentlyAnimating: Boolean, spec: FabTransformationSpec, animations: MutableList<Animator>, childBounds: RectF) {
        val translationY = calculateTranslationY(dependency, child, spec.positioning)

        val translationYTiming: MotionTiming
        if (translationY != 0.0f) {
            if ((!expanded || translationY >= 0.0f) && (expanded || translationY <= 0.0f)) {
                translationYTiming = spec.timings.getTiming("translationYCurveDownwards")
            } else {
                translationYTiming = spec.timings.getTiming("translationYCurveUpwards")
            }
        } else {
            translationYTiming = spec.timings.getTiming("translationYLinear")
        }

        val translationYAnimator: ObjectAnimator
        if (expanded) {
            if (!currentlyAnimating) {
                child.translationY = -translationY
            }

            translationYAnimator = ObjectAnimator.ofFloat(child, View.TRANSLATION_Y, *floatArrayOf(0.0f))
            calculateChildVisibleBoundsAtEndOfExpansion(child, spec, translationYTiming, -translationY, 0.0f, childBounds)
        } else {
            translationYAnimator = ObjectAnimator.ofFloat(child, View.TRANSLATION_Y, 0.0f, -translationY)
        }

        translationYTiming.apply(translationYAnimator)
        animations.add(translationYAnimator)
    }

    private fun createRotationAnimation(dependency: View, child: View, expanded: Boolean, spec: FabTransformationSpec, animations: MutableList<Animator>) {
        if (child is ViewGroup && dependency is ImageView) {
            val icon = dependency.drawable
            if (icon != null) {
                icon.mutate()
                val animator = if (expanded) {
                    ObjectAnimator.ofFloat(dependency, "rotation", 0f, 45f)
                } else {
                    ObjectAnimator.ofFloat(dependency, "rotation", 45f, 0f)
                }

                val timing = spec.timings.getTiming("rotation")
                timing.apply(animator)
                animations.add(animator)
            }
        }
    }

    private fun createChildrenFadeAnimation(child: View, expanded: Boolean, currentlyAnimating: Boolean, spec: FabTransformationSpec, animations: MutableList<Animator>) {
        if (child is ViewGroup) {
            calculateChildContentContainer(child)?.let {childContentContainer ->
                if (expanded && !currentlyAnimating) {
                    ChildrenAlphaProperty.CHILDREN_ALPHA.set(childContentContainer, 0.0f)
                }
                val childCount = child.childCount
                if (childCount > 0) {
                    for (i in 0 until childCount) {
                        val containerChild = childContentContainer.getChildAt(i)
                        if (containerChild.visibility == View.VISIBLE) {
                            animations.add(createFadeAnimator(containerChild, expanded, spec))
                        }
                    }
                }
            }
        }
    }

    private fun createFadeAnimator(view: View, expanded: Boolean, spec: FabTransformationSpec): Animator {
        val animator = if (expanded) {
            ObjectAnimator.ofFloat(view,
                "alpha", 0f, 1f)
        } else {
            ObjectAnimator.ofFloat(view,
                "alpha", 1f, 0f)
        }
        val timing = spec.timings.getTiming("contentFade")
        timing.apply(animator)
        return animator
    }

    private fun calculateTranslationY(dependency: View, child: View, positioning: Positioning):Float {
        val dependencyBounds = tmpRectF1
        val childBounds = tmpRectF2
        calculateWindowBounds(dependency, dependencyBounds)
        calculateWindowBounds(child, childBounds)

        var translationY = when (positioning.gravity and Gravity.FILL_VERTICAL) {
            Gravity.CENTER_VERTICAL -> childBounds.centerY() - dependencyBounds.centerY()
            Gravity.TOP -> childBounds.top - dependencyBounds.top
            Gravity.BOTTOM -> childBounds.bottom - dependencyBounds.bottom
            else -> 0.0f
        }
        translationY += positioning.yAdjustment

        return translationY
    }

    private fun calculateWindowBounds(view: View, rect: RectF) {
        rect.set(0.0f, 0.0f, view.width.toFloat(), view.height.toFloat())
        val windowLocation = tmpArray
        view.getLocationInWindow(windowLocation)
        rect.offsetTo(windowLocation[0].toFloat(), windowLocation[1].toFloat())
        rect.offset((-view.translationX).toInt().toFloat(), (-view.translationY).toInt().toFloat())
    }

    private fun calculateChildVisibleBoundsAtEndOfExpansion(child: View, spec: FabTransformationSpec, translationYTiming: MotionTiming, fromY: Float, toY: Float, childBounds: RectF) {
        val translationY = calculateValueOfAnimationAtEndOfExpansion(spec, translationYTiming, fromY, toY)
        val window = tmpRect
        child.getWindowVisibleDisplayFrame(window)
        val windowF = tmpRectF1
        windowF.set(window)
        val childVisibleBounds = tmpRectF2
        calculateWindowBounds(child, childVisibleBounds)
        childVisibleBounds.offset(0f, translationY)
        childVisibleBounds.intersect(windowF)
        childBounds.set(childVisibleBounds)
    }

    private fun calculateValueOfAnimationAtEndOfExpansion(spec: FabTransformationSpec, timing: MotionTiming, from: Float, to: Float): Float {
        val delay = timing.delay
        val duration = timing.duration
        val expansionTiming = spec.timings.getTiming("expansion")
        var expansionEnd = expansionTiming.delay + expansionTiming.duration
        expansionEnd += 17L
        var fraction = (expansionEnd - delay).toFloat() / duration.toFloat()
        fraction = timing.interpolator.getInterpolation(fraction)
        return AnimationUtils.lerp(from, to, fraction)
    }

    private fun calculateChildContentContainer(view: View): ViewGroup? {
        var childContentContainer:View? = view.findViewById(R.id.mtrl_child_content_container)
        if (childContentContainer != null) {
            return toViewGroupOrNull(childContentContainer)
        } else if (view !is TransformationChildLayout && view !is TransformationChildCard) {
            return toViewGroupOrNull(view)
        } else {
            childContentContainer = (view as ViewGroup).getChildAt(0)
            return toViewGroupOrNull(childContentContainer)
        }
    }

    private fun toViewGroupOrNull(view: View?): ViewGroup? = view as? ViewGroup

    private fun updateImportantForAccessibility(sheet: View, expanded: Boolean) {
        val viewParent = sheet.parent
        if (viewParent is CoordinatorLayout) {
            val childCount = viewParent.childCount
            if (expanded) {
                importantForAccessibilityMap = HashMap(childCount)
            }

            for (i in 0 until childCount) {
                val child = viewParent.getChildAt(i)
                val hasScrimBehavior = child.layoutParams is CoordinatorLayout.LayoutParams &&
                        (child.layoutParams as CoordinatorLayout.LayoutParams).behavior is FabTransformationScrimBehavior
                if (child !== sheet && !hasScrimBehavior) {
                    importantForAccessibilityMap?.let {
                        if (!expanded) {
                            if (it.containsKey(child)) {
                                ViewCompat.setImportantForAccessibility(child, it[child] as Int)
                            }
                        } else {
                            it[child] = child.importantForAccessibility

                            ViewCompat.setImportantForAccessibility(child, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS)
                        }
                    }
                }
            }

            if (!expanded) {
                importantForAccessibilityMap = null
            }

        }
    }

    private fun playTogether(set: AnimatorSet, items: List<Animator>) {
        val itemList = mutableListOf<Animator>()
        itemList.addAll(items)
        var totalDuration = 0L
        var i = 0

        val count = itemList.size
        while (i < count) {
            val animator = itemList.get(i)
            totalDuration = Math.max(totalDuration, animator.startDelay + animator.duration)
            ++i
        }

        val fix = ValueAnimator.ofInt(*intArrayOf(0, 0))
        fix.duration = totalDuration
        itemList.add(0, fix)
        set.playTogether(itemList)
    }

    data class FabTransformationSpec(val timings: MotionSpec, val positioning: Positioning)
}