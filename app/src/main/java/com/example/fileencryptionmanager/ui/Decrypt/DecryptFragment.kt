package com.example.fileencryptionmanager.ui.Decrypt

import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.lifecycle.ViewModelProvider
import com.example.fileencryptionmanager.Encrypt
import com.example.fileencryptionmanager.FeedReaderDbHelper
import com.example.fileencryptionmanager.databinding.FragmentDecryptBinding
import java.io.*
import java.lang.reflect.InvocationTargetException
import java.security.MessageDigest
import javax.crypto.BadPaddingException


class DecryptFragment : Fragment() {

    private var fileUri: Uri? = null

    private var _binding: FragmentDecryptBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentDecryptBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val selectFileButton: Button = binding.selectDecFile
        selectFileButton.setOnClickListener {
            // File manager selection
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/octet-stream"

                // Optionally, specify a URI for the file that should appear in the
                // system file picker when it loads.
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, "/")
            }
            startActivityForResult(intent, 1)
        }

        val button: Button = binding.decryptButton
        button.setOnClickListener {

            when {
                fileUri == null -> {
                    Toast.makeText(requireActivity(), "Select a file!", Toast.LENGTH_SHORT).show()
                }
                binding.editTextPasswordD.text.isEmpty() -> {
                    Toast.makeText(requireActivity(), "Enter a password!", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    DecryptData(fileUri!!)
                }
            }

        }

        return root
    }

    private fun DecryptData(uri: Uri) {

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

        //MD5 Checksum
        val md = MessageDigest
            .getInstance("MD5")
            .digest(bytes)
        val md5 = md.joinToString("") { "%02x".format(it) }

        // Select salt and iv from DB
        val dbHelper = context?.let { FeedReaderDbHelper(it) }

        // Gets the data repository in read mode
        val db = dbHelper?.readableDatabase

        val projection = arrayOf(
            FeedReaderDbHelper.FeedReaderContract.FeedEntry.MIMETYPE,
            FeedReaderDbHelper.FeedReaderContract.FeedEntry.SALT,
            FeedReaderDbHelper.FeedReaderContract.FeedEntry.IV
        )

        val selection = "${FeedReaderDbHelper.FeedReaderContract.FeedEntry.MD5SUM} = ?"
        val selectionArgs = arrayOf(md5)

        val sortOrder = "${FeedReaderDbHelper.FeedReaderContract.FeedEntry.MD5SUM} DESC"

        val cursor = db?.query(
            FeedReaderDbHelper.FeedReaderContract.FeedEntry.TABLE_NAME,   // The table to query
            projection,             // The array of columns to return (pass null to get all)
            selection,              // The columns for the WHERE clause
            selectionArgs,          // The values for the WHERE clause
            null,                   // don't group the rows
            null,                   // don't filter by row groups
            sortOrder
        )

        // Get mimetype, salt and iv
        cursor?.moveToFirst()
        val mimetype: String = cursor!!.getString(0)
        val salt: ByteArray = cursor.getBlob(1)
        val iv: ByteArray = cursor.getBlob(2)
        cursor.close()


        // Get password
        val password: EditText = binding.editTextPasswordD

        val wrongPassword = Toast.makeText(requireActivity(), "Wrong password!", Toast.LENGTH_SHORT)

        try {
            //Decrypt
            val decData = Encrypt.decrypt(
                password.text.toString(),
                salt,
                iv,
                bytes!!
            )

            // Set decrypted data to a static variable
            Encrypt.decDataStatic = decData

            // Create decrypted file
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = mimetype
                putExtra(Intent.EXTRA_TITLE, "decfile")

                // Optionally, specify a URI for the directory that should be opened in
                // the system file picker before your app creates the document.
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, "/")
            }
            startActivityForResult(intent, 2)

            if (binding.delCheckBox.isChecked) {
                // Delete encrypted file
                DocumentsContract.deleteDocument(requireActivity().contentResolver, uri)

                //Delete uri row from DB
                val deletedRows = db.delete(
                    FeedReaderDbHelper.FeedReaderContract.FeedEntry.TABLE_NAME,
                    selection,
                    selectionArgs
                )
            }

        } catch(e: RuntimeException) {
            wrongPassword.show()
        } catch (e: InvocationTargetException) {
            wrongPassword.show()
        } catch (e: BadPaddingException) {
            wrongPassword.show()
        }



    }

    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {

        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            resultData?.data?.also { uri ->
                fileUri = uri
                binding.selectDecFile.text = "{ ${File(uri.path!!).name} }"
            }
        } else if (requestCode == 2 && resultCode == Activity.RESULT_OK) {

            //Set to null fileUri
            fileUri = null

            resultData?.data.also { uri ->
                try {
                    if (uri != null) {
                        requireActivity().contentResolver.openFileDescriptor(uri, "w")?.use { it ->
                            FileOutputStream(it.fileDescriptor).use {
                                it.write(Encrypt.decDataStatic)
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