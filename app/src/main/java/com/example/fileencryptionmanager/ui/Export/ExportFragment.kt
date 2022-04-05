package com.example.fileencryptionmanager.ui.Export;

import android.app.Activity
import android.content.Intent
import android.database.CursorIndexOutOfBoundsException
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.fileencryptionmanager.Encrypt
import com.example.fileencryptionmanager.FeedReaderDbHelper
import com.example.fileencryptionmanager.databinding.FragmentExportBinding
import com.example.prova.ui.notifications.ExportViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class ExportFragment : Fragment() {

    private var json: JSONArray? = null

    private var _binding: FragmentExportBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this).get(ExportViewModel::class.java)

        _binding = FragmentExportBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val exportButton: Button = binding.exportButton
        exportButton.setOnClickListener {

            // Get data from DB
            val dbHelper = context?.let { FeedReaderDbHelper(it) }

            // Gets the data repository in read mode
            val db = dbHelper?.readableDatabase

            val projection = arrayOf(
                FeedReaderDbHelper.FeedReaderContract.FeedEntry.MD5SUM,
                FeedReaderDbHelper.FeedReaderContract.FeedEntry.MIMETYPE,
                FeedReaderDbHelper.FeedReaderContract.FeedEntry.SALT,
                FeedReaderDbHelper.FeedReaderContract.FeedEntry.IV
            )

            val cursor = db?.query(
                FeedReaderDbHelper.FeedReaderContract.FeedEntry.TABLE_NAME,   // The table to query
                projection,             // The array of columns to return (pass null to get all)
                null,              // The columns for the WHERE clause
                null,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null
            )

            // Get data
            try {
                cursor?.moveToFirst()

                val jsonString = StringBuilder("[")
                while (cursor!!.moveToNext()) {
                    val md5 = cursor.getString(0)
                    val mimetype = cursor.getString(1)
                    val salt = cursor.getBlob(2).contentToString()
                    val iv = cursor.getBlob(3).contentToString()
                    if (cursor.isLast) {
                        jsonString.append("[\"$md5\", \"$mimetype\", $salt, $iv]")
                    } else {
                        jsonString.append("[\"$md5\", \"$mimetype\", $salt, $iv],")
                    }

                }
                jsonString.append("]")

                val jsonArray = JSONArray(jsonString.toString())
                json = jsonArray

                cursor.close()

                // Create database json file
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    putExtra(Intent.EXTRA_TITLE, "database")

                    // Optionally, specify a URI for the directory that should be opened in
                    // the system file picker before your app creates the document.
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, "/")
                }
                startActivityForResult(intent, 1)

            } catch (e: CursorIndexOutOfBoundsException) {
                Toast.makeText(requireActivity(), "No data in DB", Toast.LENGTH_SHORT).show()
                cursor?.close()
            }

        }

        return root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {

        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            resultData?.data?.also { uri ->
                try {
                    requireActivity().contentResolver.openFileDescriptor(uri, "w")?.use {
                        FileOutputStream(it.fileDescriptor).use {
                            it.write(json.toString().toByteArray())
                            it.flush()
                            it.close()
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