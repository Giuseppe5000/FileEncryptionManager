package com.example.fileencryptionmanager.ui.Encrypt

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.fileencryptionmanager.Encrypt
import com.example.fileencryptionmanager.databinding.FragmentEncryptBinding
import java.io.*

class EncryptFragment : Fragment() {

    private var _binding: FragmentEncryptBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("GetInstance")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentEncryptBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val button: Button = binding.button
        button.setOnClickListener {

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

        return root
    }

    fun EncryptData(uri: Uri) {

        // Read file
        val stringBuilder = StringBuilder()
        requireActivity().contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = reader.readLine()
                }
            }
        }

        //Get password
        val password: EditText = binding.editTextPassword

        //Encrypt
        val encData =
            Encrypt.encrypt(password.text.toString(), stringBuilder.toString().toByteArray())

        // Set encrypted data to a static varible
        Encrypt.encDataStatic = encData.encryptedData!!

        // Create encrypted file
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/txt"
            putExtra(Intent.EXTRA_TITLE, "file.enc")

            // Optionally, specify a URI for the directory that should be opened in
            // the system file picker before your app creates the document.
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, "/")
            putExtra("ENC_DATA", encData.encryptedData)
        }
        startActivityForResult(intent, 2)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {

        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            resultData?.data?.also { uri ->
                EncryptData(uri)
            }
        } else if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            resultData?.data.also { uri ->
                try {
                    if (uri != null) {
                        requireActivity().contentResolver.openFileDescriptor(uri, "w")?.use { it ->
                            FileOutputStream(it.fileDescriptor).use {
                                it.write(Encrypt.encDataStatic)
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