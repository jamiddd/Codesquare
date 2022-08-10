package com.jamid.codesquare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.codesquare.BaseFragment
import com.jamid.codesquare.databinding.FragmentMaintenanceBinding
import com.jamid.codesquare.toast

class MaintenanceFragment: BaseFragment<FragmentMaintenanceBinding>() {

    override fun onCreateBinding(inflater: LayoutInflater): FragmentMaintenanceBinding {
        return FragmentMaintenanceBinding.inflate(inflater)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        solveCompatibilityIssuesWithMediaItems()

    }

    private fun solveCompatibilityIssuesWithMediaItems() {
        Firebase.firestore.collection("posts").get()
            .addOnSuccessListener {
                if (!it.isEmpty) {
                    val batch = Firebase.firestore.batch()

                    for (doc in it.documents) {
                        val images = doc["images"] as List<String>

                        val newChange = mapOf(
                            "mediaList" to images,
                            "images" to FieldValue.delete()
                        )

                        batch.update(doc.reference, newChange)
                    }

                    batch.commit()
                        .addOnSuccessListener {
                            toast("Successfully updated.")
                            activity.finish()
                        }.addOnFailureListener {
                            toast("Couldn't update.")
                        }
                }

            }.addOnFailureListener {
                toast("Something went wrong: ${it.localizedMessage}")
            }
    }

}