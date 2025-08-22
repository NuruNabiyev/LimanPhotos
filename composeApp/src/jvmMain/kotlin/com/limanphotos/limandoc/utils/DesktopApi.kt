package com.limanphotos.limandoc.utils

// import saschpe.log4k.println // Removed due to library update
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.Locale


object DesktopApi {
    //private val LOG: printlnger = printlngerFactory.getprintlnger(Desktop::class.java)

    fun browse(uri: URI): Boolean {
        if (browseDESKTOP(uri)) {
            return true
        }
        if (getOs().isLinux) {
            if (browseLinux(File(uri))) {
                return true
            }
        }
        if (openSystemSpecific(uri.toString())) {
            return true
        }
        println(String.format("failed to browse %s", uri))
        return false
    }


    fun open(file: File): Boolean {
        if (openDESKTOP(file)) {
            return true
        }
        if (openSystemSpecific(file.path)) {
            return true
        }
        println(String.format("failed to open %s", file.absolutePath))
        return false
    }


    private fun browseLinux(file: File): Boolean {
        if (runCommand("xdg-open", "%s", file.parent)) {
            return true
        }

        if (runCommand("kde-open", "%s", file.parent)) {
            return true
        }

        if (runCommand("gnome-open", "%s", file.parent)) {
            return true
        }

        if (runCommand("kde-open", "%s", file.parent)) {
            return true
        }
        if (runCommand("gnome-open", "%s", file.parent)) {
            return true
        }
        return false
    }

    private fun openSystemSpecific(what: String): Boolean {
        val os = getOs()
        if (os.isLinux) {
            if (isXDG()) {
                if (runCommand("xdg-open", "%s", what)) {
                    return true
                }
            }
            if (isKDE()) {
                if (runCommand("kde-open", "%s", what)) {
                    return true
                }
            }
            if (isGNOME()) {
                if (runCommand("gnome-open", "%s", what)) {
                    return true
                }
            }
            if (runCommand("kde-open", "%s", what)) {
                return true
            }
            if (runCommand("gnome-open", "%s", what)) {
                return true
            }
        }
        if (os.isMac) {
            if (runCommand("open", "%s", what)) {
                return true
            }
        }
        if (os.isWindows) {
            if (runCommand("explorer", "%s", what)) {
                return true
            }
        }
        return false
    }


    private fun browseDESKTOP(uri: URI): Boolean {
        return try {
            val os = getOs()
            if (os.isWindows) {
                runCommand("explorer.exe", "%s", "/select,\"$uri\"")
                return true
            }
            if (!Desktop.isDesktopSupported()) {
                println("Platform is not supported.")
                return false
            }
            if (!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                println("BROWSE is not supported.")
                return false
            }
            println("Trying to use Desktop.getDesktop().browse() with $uri")
            Desktop.getDesktop().browseFileDirectory(File(uri))
            true
        } catch (t: Throwable) {
            println("Error using desktop browse.")
            t.printStackTrace()
            false
        }
    }


    private fun openDESKTOP(file: File): Boolean {
        return try {
            if (!Desktop.isDesktopSupported()) {
                println("Platform is not supported.")
                return false
            }
            if (!Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                println("OPEN is not supported.")
                return false
            }
            println("Trying to use Desktop.getDesktop().open() with $file")
            Desktop.getDesktop().open(file)
            true
        } catch (t: Throwable) {
            println("Error using desktop open.")
            t.printStackTrace()
            false
        }
    }


    private fun editDESKTOP(file: File): Boolean {
        return try {
            if (!Desktop.isDesktopSupported()) {
                println("Platform is not supported.")
                return false
            }
            if (!Desktop.getDesktop().isSupported(Desktop.Action.EDIT)) {
                println("EDIT is not supported.")
                return false
            }
            println("Trying to use Desktop.getDesktop().edit() with $file")
            Desktop.getDesktop().edit(file)
            true
        } catch (t: Throwable) {
            println("Error using desktop edit.")
            t.printStackTrace()
            false
        }
    }


    private fun runCommand(command: String, args: String, file: String): Boolean {
        println("Trying to exec:\n   cmd = $command\n   args = $args\n   %s = $file")
        val parts = prepareCommand(command, args, file)
        return try {
            val p = Runtime.getRuntime().exec(parts) ?: return false
            try {
                val retval = p.exitValue()
                if (retval == 0) {
                    println("Process ended immediately.")
                    false
                } else {
                    println("Process crashed.")
                    false
                }
            } catch (itse: IllegalThreadStateException) {
                println("Process is running.")
                true
            }
        } catch (e: IOException) {
            println("Error running command.")
            e.printStackTrace()
            false
        }
    }


    private fun prepareCommand(command: String, args: String?, file: String): Array<String> {
        val parts: MutableList<String> = ArrayList()
        parts.add(command)
        if (args != null) {
            for (s in args.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                val str = String.format(s, file) // put in the filename thing
                parts.add(str.trim { it <= ' ' })
            }
        }
        return parts.toTypedArray<String>()
    }

    private fun isXDG(): Boolean {
        val xdgSessionId = System.getenv("XDG_SESSION_ID")
        return xdgSessionId != null && !xdgSessionId.isEmpty()
    }

    private fun isGNOME(): Boolean {
        val gdmSession = System.getenv("GDMSESSION")
        return gdmSession != null && gdmSession.lowercase(Locale.getDefault()).contains("gnome")
    }

    private fun isKDE(): Boolean {
        val gdmSession = System.getenv("GDMSESSION")
        return gdmSession != null && gdmSession.lowercase(Locale.getDefault()).contains("kde")
    }


    enum class EnumOS {
        linux,
        macos,
        solaris,
        unknown,
        windows;

        val isLinux: Boolean
            get() = this == linux || this == solaris
        val isMac: Boolean
            get() = this == macos
        val isWindows: Boolean
            get() = this == windows
    }


    fun getOs(): EnumOS {
        val s = System.getProperty("os.name").lowercase(Locale.getDefault())
        if (s.contains("win")) {
            return EnumOS.windows
        }
        if (s.contains("mac")) {
            return EnumOS.macos
        }
        if (s.contains("solaris")) {
            return EnumOS.solaris
        }
        if (s.contains("sunos")) {
            return EnumOS.solaris
        }
        if (s.contains("linux")) {
            return EnumOS.linux
        }
        return if (s.contains("unix")) {
            EnumOS.linux
        } else {
            EnumOS.unknown
        }
    }


}