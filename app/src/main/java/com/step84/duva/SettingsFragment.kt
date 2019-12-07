package com.step84.duva

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [SettingsFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [SettingsFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class SettingsFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null

    private val TAG = "SettingsFragment"

    private lateinit var auth: FirebaseAuth
    private lateinit var btn_signInAnonymous: Button
    private lateinit var btn_signIn: Button
    private lateinit var btn_signOut: Button

    private val providers = arrayListOf(
        AuthUI.IdpConfig.EmailBuilder().build(),
        AuthUI.IdpConfig.GoogleBuilder().build()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        btn_signInAnonymous = view.findViewById(R.id.btn_signInAnonymous)
        btn_signIn = view.findViewById(R.id.btn_signIn)
        btn_signOut = view.findViewById(R.id.btn_signout)

        if(auth.currentUser == null) {
            btn_signInAnonymous.visibility = View.VISIBLE
            btn_signIn.visibility = View.VISIBLE
            btn_signOut.visibility = View.INVISIBLE
        } else {
            btn_signInAnonymous.visibility = View.INVISIBLE
            btn_signIn.visibility = View.INVISIBLE
            btn_signOut.visibility = View.VISIBLE
        }

        btn_signInAnonymous.setOnClickListener {
            Toast.makeText(activity, "Sign in anonymously...", Toast.LENGTH_LONG).show()
            auth.signInAnonymously()
                .addOnCompleteListener { task: Task<AuthResult> ->
                    if(task.isSuccessful) {
                        Log.d(TAG, "duva: user logged in anonymously")
                        val newUser: User = User(uid = auth.uid.toString(), active = true)
                        Firestore.addUser(newUser, object: FirestoreCallback {
                            override fun onSuccess() {
                                Log.i(TAG, "duva: user anonymous user successfully added to database")
                            }
                            override fun onFailed() {
                                Log.d(TAG, "duva: user anonymous failed to add to database")
                            }
                        })
                        btn_signInAnonymous.visibility = View.INVISIBLE
                        btn_signOut.visibility = View.VISIBLE
                    } else {
                        Log.d(TAG, "duva: user failed to login anonymously")
                    }
                }
        }

        btn_signIn.setOnClickListener {
            Toast.makeText(activity, "Sign in...", Toast.LENGTH_LONG).show()
            startActivityForResult(
                AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .build(),
                200)
            btn_signIn.visibility = View.INVISIBLE
            btn_signOut.visibility = View.VISIBLE
        }

        btn_signOut.setOnClickListener {
            AuthUI.getInstance()
                .signOut(context!!)
                .addOnCompleteListener {
                    Toast.makeText(activity, "Signed out", Toast.LENGTH_LONG).show()
                    if(Globals.currentUser != null) {
                        Firestore.resetActiveSubscriptions(Globals.currentUser!!.uid, object: FirestoreCallback {
                            override fun onSuccess() {
                                Globals.currentUser = null
                                Globals.currentSubscriptions = null
                            }
                            override fun onFailed() {}
                        })
                    }

                    btn_signIn.visibility = View.VISIBLE
                    btn_signOut.visibility = View.INVISIBLE
                }
        }

        return view
    }

    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SettingsFragment.
         */
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SettingsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
