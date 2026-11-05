using System.Diagnostics;
using System.Text;
using System.Text.RegularExpressions;
using System.Windows.Forms;

namespace StarClassLoader;

internal static class Program
{
    [STAThread]
    private static void Main()
    {
        Application.EnableVisualStyles();
        Application.SetCompatibleTextRenderingDefault(false);
        Application.Run(new LauncherForm());
    }
}

internal sealed class LauncherForm : Form
{
    private readonly TextBox _javaPath = new() { Dock = DockStyle.Fill };
    private readonly TextBox _targetPath = new() { Dock = DockStyle.Fill };
    private readonly TextBox _output = new() { Dock = DockStyle.Fill, Multiline = true, ReadOnly = true,
        ScrollBars = ScrollBars.Vertical, WordWrap = false };
    private readonly Button _loadButton = new() { Text = "Validar e carregar classes", AutoSize = true };
    private readonly Button _stopButton = new() { Text = "Parar", AutoSize = true, Enabled = false };
    private Process _javaProcess;

    public LauncherForm()
    {
        Text = "Star Class Loader — JVM de teste Java 8";
        Width = 900;
        Height = 640;
        MinimumSize = new Size(760, 500);
        StartPosition = FormStartPosition.CenterScreen;

        var layout = new TableLayoutPanel
        {
            Dock = DockStyle.Fill,
            Padding = new Padding(12),
            ColumnCount = 1,
            RowCount = 5
        };
        layout.RowStyles.Add(new RowStyle(SizeType.Absolute, 28));
        layout.RowStyles.Add(new RowStyle(SizeType.Absolute, 54));
        layout.RowStyles.Add(new RowStyle(SizeType.Absolute, 54));
        layout.RowStyles.Add(new RowStyle(SizeType.Absolute, 42));
        layout.RowStyles.Add(new RowStyle(SizeType.Percent, 100));
        Controls.Add(layout);

        var header = new Label
        {
            Text = "Carregamento isolado: o alvo roda em uma JVM filha; nenhum processo Lunar é localizado ou alterado.",
            Dock = DockStyle.Fill,
            TextAlign = ContentAlignment.MiddleLeft
        };
        layout.Controls.Add(header, 0, 0);

        layout.Controls.Add(BuildPathRow("Java 8 (java.exe)", _javaPath, BrowseJava), 0, 1);
        layout.Controls.Add(BuildPathRow("JAR ou diretório de classes", _targetPath, null), 0, 2);

        var actions = new FlowLayoutPanel { Dock = DockStyle.Fill, FlowDirection = FlowDirection.LeftToRight };
        var browseJar = new Button { Text = "Selecionar JAR…", AutoSize = true };
        browseJar.Click += (_, _) => BrowseJar();
        var browseDirectory = new Button { Text = "Selecionar diretório…", AutoSize = true };
        browseDirectory.Click += (_, _) => BrowseDirectory();
        _loadButton.Click += (_, _) => StartLoad();
        _stopButton.Click += (_, _) => StopLoad();
        actions.Controls.Add(browseJar);
        actions.Controls.Add(browseDirectory);
        actions.Controls.Add(_loadButton);
        actions.Controls.Add(_stopButton);
        layout.Controls.Add(actions, 0, 3);
        layout.Controls.Add(_output, 0, 4);

        _javaPath.Text = FindJava8() ?? string.Empty;
        AppendLog("Selecione um JAR ou diretório de classes autorizado. O conteúdo é carregado sem executar inicializadores estáticos.");
        if (_javaPath.Text.Length == 0)
            AppendLog("Java 8 não localizado automaticamente; selecione java.exe do JDK/JRE 8.");
    }

    private static Control BuildPathRow(string label, TextBox textBox, Action browse)
    {
        var row = new TableLayoutPanel { Dock = DockStyle.Fill, ColumnCount = 3, RowCount = 2 };
        row.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 70));
        row.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 20));
        row.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 10));
        row.RowStyles.Add(new RowStyle(SizeType.Absolute, 20));
        row.RowStyles.Add(new RowStyle(SizeType.Percent, 100));
        row.Controls.Add(new Label { Text = label, Dock = DockStyle.Fill, TextAlign = ContentAlignment.MiddleLeft }, 0, 0);
        row.SetColumnSpan(row.GetControlFromPosition(0, 0), 3);
        row.Controls.Add(textBox, 0, 1);
        if (browse != null)
        {
            var button = new Button { Text = "Procurar…", Dock = DockStyle.Right, AutoSize = true };
            button.Click += (_, _) => browse();
            row.SetColumnSpan(textBox, 2);
            row.Controls.Add(button, 2, 1);
        }
        else row.SetColumnSpan(textBox, 3);
        return row;
    }

    private void BrowseJava()
    {
        using var dialog = new OpenFileDialog { Filter = "Java executable (java.exe)|java.exe", CheckFileExists = true };
        if (dialog.ShowDialog(this) == DialogResult.OK)
            _javaPath.Text = dialog.FileName;
    }

    private void BrowseJar()
    {
        using var dialog = new OpenFileDialog { Filter = "Java archive (*.jar)|*.jar", CheckFileExists = true };
        if (dialog.ShowDialog(this) == DialogResult.OK)
            _targetPath.Text = dialog.FileName;
    }

    private void BrowseDirectory()
    {
        using var dialog = new FolderBrowserDialog { Description = "Selecione a raiz do diretório de classes" };
        if (dialog.ShowDialog(this) == DialogResult.OK)
            _targetPath.Text = dialog.SelectedPath;
    }

    private async void StartLoad()
    {
        if (_javaProcess != null)
        {
            AppendLog("Já existe uma JVM de teste em execução.");
            return;
        }

        string javaPath = _javaPath.Text.Trim();
        string targetPath = _targetPath.Text.Trim();
        string toolJar = Path.Combine(AppContext.BaseDirectory, "StarClassLoader.jar");
        if (!File.Exists(toolJar))
        {
            AppendLog("JAR do carregador não encontrado ao lado do EXE: " + toolJar);
            return;
        }
        if (!File.Exists(javaPath) || !IsJava8(javaPath, out string javaVersion))
        {
            AppendLog("Selecione um java.exe válido do Java 8.");
            return;
        }
        if (!(File.Exists(targetPath) && string.Equals(Path.GetExtension(targetPath), ".jar", StringComparison.OrdinalIgnoreCase)) &&
            !Directory.Exists(targetPath))
        {
            AppendLog("O alvo precisa ser um arquivo .jar existente ou um diretório de classes existente.");
            return;
        }

        string logDirectory = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "StarClassLoader", "logs");
        Directory.CreateDirectory(logDirectory);
        string logPath = Path.Combine(logDirectory, "classloader-" + DateTime.Now.ToString("yyyyMMdd-HHmmss") + ".log");
        var start = new ProcessStartInfo
        {
            FileName = javaPath,
            WorkingDirectory = AppContext.BaseDirectory,
            UseShellExecute = false,
            CreateNoWindow = true,
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            StandardOutputEncoding = Encoding.UTF8,
            StandardErrorEncoding = Encoding.UTF8
        };
        start.ArgumentList.Add("-jar");
        start.ArgumentList.Add(toolJar);
        start.ArgumentList.Add("--target");
        start.ArgumentList.Add(Path.GetFullPath(targetPath));
        start.ArgumentList.Add("--log");
        start.ArgumentList.Add(logPath);

        try
        {
            var process = new Process { StartInfo = start, EnableRaisingEvents = true };
            process.OutputDataReceived += (_, eventArgs) => { if (eventArgs.Data != null) AppendLog(eventArgs.Data); };
            process.ErrorDataReceived += (_, eventArgs) => { if (eventArgs.Data != null) AppendLog("ERR: " + eventArgs.Data); };
            process.Exited += (_, _) =>
            {
                int exitCode;
                try { exitCode = process.ExitCode; } catch { exitCode = -1; }
                BeginInvoke(new Action(() =>
                {
                    AppendLog("JVM finalizada (código " + exitCode + "). Log: " + logPath);
                    _javaProcess?.Dispose();
                    _javaProcess = null;
                    _loadButton.Enabled = true;
                    _stopButton.Enabled = false;
                }));
            };
            if (!process.Start())
            {
                process.Dispose();
                AppendLog("Não foi possível iniciar a JVM de teste.");
                return;
            }
            _javaProcess = process;
            _loadButton.Enabled = false;
            _stopButton.Enabled = true;
            process.BeginOutputReadLine();
            process.BeginErrorReadLine();
            AppendLog("JVM filha iniciada: " + javaVersion);
            AppendLog("Alvo: " + Path.GetFullPath(targetPath));
            AppendLog("Log: " + logPath);
            await process.WaitForExitAsync();
        }
        catch (Exception error)
        {
            AppendLog("Falha ao iniciar o carregador: " + error.Message);
            _javaProcess?.Dispose();
            _javaProcess = null;
            _loadButton.Enabled = true;
            _stopButton.Enabled = false;
        }
    }

    private void StopLoad()
    {
        try
        {
            if (_javaProcess != null && !_javaProcess.HasExited)
            {
                _javaProcess.Kill(true);
                AppendLog("JVM filha encerrada.");
            }
        }
        catch (Exception error)
        {
            AppendLog("Falha ao encerrar JVM filha: " + error.Message);
        }
    }

    private void AppendLog(string message)
    {
        if (IsDisposed) return;
        if (InvokeRequired)
        {
            BeginInvoke(new Action(() => AppendLog(message)));
            return;
        }
        _output.AppendText(DateTime.Now.ToString("HH:mm:ss ") + message + Environment.NewLine);
    }

    private static string FindJava8()
    {
        var candidates = new List<string>();
        string javaHome = Environment.GetEnvironmentVariable("JAVA_HOME");
        if (!string.IsNullOrWhiteSpace(javaHome))
            candidates.Add(Path.Combine(javaHome, "bin", "java.exe"));
        string path = Environment.GetEnvironmentVariable("PATH") ?? string.Empty;
        candidates.AddRange(path.Split(Path.PathSeparator).Select(item => Path.Combine(item.Trim(), "java.exe")));
        foreach (string root in new[] { Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles) + "\\Java",
                     Environment.GetFolderPath(Environment.SpecialFolder.ProgramFilesX86) + "\\Java" })
        {
            if (!Directory.Exists(root)) continue;
            foreach (string directory in Directory.GetDirectories(root).Where(item =>
                         Path.GetFileName(item).StartsWith("jdk1.8", StringComparison.OrdinalIgnoreCase) ||
                         Path.GetFileName(item).StartsWith("jre1.8", StringComparison.OrdinalIgnoreCase)))
                candidates.Add(Path.Combine(directory, "bin", "java.exe"));
        }
        return candidates.FirstOrDefault(candidate => File.Exists(candidate) && IsJava8(candidate, out _));
    }

    private static bool IsJava8(string javaPath, out string version)
    {
        version = string.Empty;
        try
        {
            var start = new ProcessStartInfo
            {
                FileName = javaPath,
                Arguments = "-version",
                UseShellExecute = false,
                CreateNoWindow = true,
                RedirectStandardError = true,
                RedirectStandardOutput = true
            };
            using var process = Process.Start(start);
            if (process == null) return false;
            string stderr = process.StandardError.ReadToEnd();
            string stdout = process.StandardOutput.ReadToEnd();
            if (!process.WaitForExit(5000))
            {
                process.Kill(true);
                return false;
            }
            version = (stderr + Environment.NewLine + stdout).Trim();
            return process.ExitCode == 0 && Regex.IsMatch(version, "(?:java|openjdk) version \\\"1\\.8\\.0(?:_[0-9]+)?\\\"",
                RegexOptions.IgnoreCase | RegexOptions.CultureInvariant);
        }
        catch
        {
            return false;
        }
    }
}
