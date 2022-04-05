package com.example.fileencryptionmanager.ui.Encrypt

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.fileencryptionmanager.Encrypt
import com.example.fileencryptionmanager.FeedReaderDbHelper
import com.example.fileencryptionmanager.databinding.FragmentEncryptBinding
import java.io.*
import java.security.MessageDigest

class EncryptFragment : Fragment() {

    private var _binding: FragmentEncryptBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentEncryptBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val button: Button = binding.encryptButton
        button.setOnClickListener {

            if (binding.editTextPasswordE.text.isEmpty()) {
                Toast.makeText(requireActivity(), "Enter a password!", Toast.LENGTH_SHORT).show()
            } else {
                // File manager selection
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"

                    // Optionally, specify a URI for the file that should appear in the
                    // system file picker when it loads.
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, "/")
                }
                startActivityForResult(intent, 1)
            }

        }

        return root
    }

    private fun EncryptData(uri: Uri) {

        // Read file
        var bytes: ByteArray? = null
        requireActivity().contentResolver.openFileDescriptor(uri, "r").use { it ->
            FileInputStream(
                it?.fileDescriptor
            ).use {
                bytes = it.readBytes()
                it.close()
            }
        }

        //Get password
        val password: EditText = binding.editTextPasswordE

        //Encrypt
        val encData =
            Encrypt.encrypt(password.text.toString(), bytes!!)

        // Set encrypted data and mimetype to a static variable
        Encrypt.encDataStatic = encData
        Encrypt.encDataStatic!!.mimetype = requireActivity().contentResolver.getType(uri)!!

        // Create encrypted file
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_TITLE, "file")

            // Optionally, specify a URI for the directory that should be opened in
            // the system file picker before your app creates the document.
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, "/")
        }
        startActivityForResult(intent, 2)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {

        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {

            // The result data contains a URI for the document or directory that
            // the user selected.
            resultData?.data.also { uri ->
                uri?.let { EncryptData(it) }
            }
        } else if (requestCode == 2 && resultCode == Activity.RESULT_OK) {

            //MD5 Checksum
            val md = MessageDigest
                .getInstance("MD5")
                .digest(Encrypt.encDataStatic!!.encryptedData)
            val md5 = md.joinToString("") { "%02x".format(it) }

            // SQLite
            val dbHelper = context?.let { FeedReaderDbHelper(it) }

            // Gets the data repository in write mode
            val db = dbHelper?.writableDatabase

            val values = ContentValues().apply {
                put(
                    FeedReaderDbHelper.FeedReaderContract.FeedEntry.MD5SUM,
                    md5
                )
                put(
                    FeedReaderDbHelper.FeedReaderContract.FeedEntry.MIMETYPE,
                    Encrypt.encDataStatic!!.mimetype
                )
                put(
                    FeedReaderDbHelper.FeedReaderContract.FeedEntry.SALT,
                    Encrypt.encDataStatic?.salt
                )
                put(
                    FeedReaderDbHelper.FeedReaderContract.FeedEntry.IV,
                    Encrypt.encDataStatic?.iv
                )
            }

            // Insert the new row, returning the primary key value of the new row
            val newRowId = db?.insert(
                FeedReaderDbHelper.FeedReaderContract.FeedEntry.TABLE_NAME,
                null,
                values
            )



            resultData?.data.also { uri ->
                try {
                    if (uri != null) {
                        requireActivity().contentResolver.openFileDescriptor(uri, "w")?.use { it ->
                            FileOutputStream(it.fileDescriptor).use {
                                it.write(Encrypt.encDataStatic?.encryptedData)
                                it.flush()
                                it.close()
                            }
                        }
                    }

                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}