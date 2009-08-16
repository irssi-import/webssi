use strict;
use JSON;
use HTTP::Response;
use HTTP::Parser;
use IO::Socket;
use IO::Socket::SSL;
use POSIX qw(errno_h BUFSIZ);

use Irssi;
eval "use Irssi::TextUI;"; # just to make my editor (Eclipse EPIC plugin) happy

########## SESSIONS ##########

my %sessions;

sub new_session {
	my $sessionid = "S" . int(rand(1000000000000000));
	my $session = {id => $sessionid, events => [], creation_time => time()};
	$sessions{$sessionid} = $session;
	add_init_events($session);
	#debug("creating new session:" . Dumper($session));
	return $sessions{$sessionid};
}

sub remove_session($) {
	my ($session) = @_;
	$session->{removed} = 1;
	delete $sessions{$session->{'id'}};
	clear_session_bookmarks($session);
}

sub sessions() {
	return values(%sessions);
}

########## IO ##########

Irssi::settings_add_str('webssi', 'webssi_password', '');
Irssi::settings_add_int('webssi', 'webssi_http_port', -1);
Irssi::settings_add_int('webssi', 'webssi_https_port', -1);
Irssi::settings_add_str('webssi', 'webssi_https_key_file', Irssi::get_irssi_dir()."/webssi-key.pem");
Irssi::settings_add_str('webssi', 'webssi_https_cert_file', Irssi::get_irssi_dir()."/webssi-cert.pem");

my ($settings_http_port, $settings_https_port, $settings_https_key_file, $settings_https_cert_file, $settings_password);
my ($http_listen_socket, $https_listen_socket);

my %connections;

# Called when loading, or when the settings have changed
# (re)starts the http and/or https listening socket if forced or if relevant settings have changed
sub refresh_servers {
	my ($force) = @_;
	
	if ($force || $settings_password ne Irssi::settings_get_str('webssi_password')) {
		if (Irssi::settings_get_str('webssi_password') eq '') {
			# if the password previously wasn't set, force start of server even when other settings haven't changed
			$force = 1;
		}
		$settings_password = Irssi::settings_get_str('webssi_password');
		if ($settings_password eq '') {
			Irssi::print("No password configured for webssi. Use /SET webssi_password <password>");
		}
	}
	
	if ($force || $settings_http_port != Irssi::settings_get_int('webssi_http_port')) {
		if ($http_listen_socket) {
			Irssi::print("Stopping Webssi HTTP server on port $settings_http_port");
			$http_listen_socket->close();
			$http_listen_socket = undef;
		}
		
		$settings_http_port = Irssi::settings_get_int('webssi_http_port');
		
		if ($settings_http_port > 0 && $settings_password ne '') {
			$http_listen_socket = IO::Socket::INET->new(
				Proto => 'tcp',
				LocalPort => $settings_http_port,
				Listen => 4,
				Reuse => 1,
				Blocking => 0,
				LocalAddr => "127.0.0.1"
			);
				
			if (! $http_listen_socket) {
				Irssi::print("Failed to start Webssi HTTP server: " . $!);
			} else {
				Irssi::print("Webssi HTTP server listening on port $settings_http_port (for security reasons http can only be used from localhost)");
				Irssi::input_add(fileno($http_listen_socket), INPUT_READ, \&handle_http_connection, 0);
			}
		}
	}

	if ($force || $settings_https_port != Irssi::settings_get_int('webssi_https_port')
			|| $settings_https_key_file ne Irssi::settings_get_str('webssi_https_key_file')
			|| $settings_https_cert_file ne Irssi::settings_get_str('webssi_https_cert_file')) {
		if ($https_listen_socket) {
			Irssi::print("Stopping Webssi HTTPS server on port $settings_https_port");
			$https_listen_socket->close();
			$https_listen_socket = undef;
		}
		
		$settings_https_port = Irssi::settings_get_int('webssi_https_port');
		$settings_https_key_file = Irssi::settings_get_str('webssi_https_key_file');
		$settings_https_cert_file = Irssi::settings_get_str('webssi_https_cert_file');
		
		if ($settings_https_port > 0 && $settings_password ne '') {
			if (! -e $settings_https_key_file || ! -e $settings_https_cert_file) {
				Irssi::print("No key or certificate file found for https server. You can generate them by executing the following commands in a shell: \n"
					. "   openssl req -x509 -days 3650 -new -nodes -keyout $settings_https_key_file -out $settings_https_cert_file -batch \n"
					. "   chmod 600 $settings_https_key_file $settings_https_cert_file\n"
					. " and then do /WEBSSI RESET in irssi\n"
					. " The first time you connect with your browser, you will get a security warning that the certificate cannot be validated."
					. " That is normal because no authority has signed the certificate you just created.\n"
					. " You should tell your browser to trust this certificate (after all, you created it yourself)."
					. " If you are careful/paranoid, you can compare the fingerprint shown in your browser to the output of:\n"
					. "   openssl x509 -noout -in $settings_https_cert_file -fingerprint");
			} else {
				$https_listen_socket = IO::Socket::INET->new(
					Proto => 'tcp',
					LocalPort => $settings_https_port,
					Listen => 4,
					Reuse => 1,
					Blocking => 0
				);
				if (! $https_listen_socket) {
					Irssi::print("Failed to start Webssi HTTPS server: " . $!);
				} else {
					Irssi::print("Webssi HTTPS server listening on port $settings_https_port");
					Irssi::input_add(fileno($https_listen_socket), INPUT_READ, \&handle_https_connection, 0);
				}
			}
		}
	}
	
	if ($settings_http_port <= 0 && $settings_https_port <= 0) {
		Irssi::print("No listening port configured for webssi. Use /SET webssi_https_port <port number>");
	}
}

sub handle_http_connection {
	my $connection = handle_connection($http_listen_socket);
	if ($connection) {
		connection_watch_read($connection);
	}
}

sub handle_https_connection {
	my $connection = handle_connection($https_listen_socket);
	if ($connection) {
		IO::Socket::SSL->start_SSL($connection->{socket},
			SSL_startHandshake => 0, # don't do a blocking handshake
			SSL_server => 1,
			SSL_key_file => $settings_https_key_file,
			SSL_cert_file => $settings_https_cert_file
		);
		accept_ssl($connection);
	}
}

sub handle_connection($) {
	my ($listen_socket) = @_;
	my $socket = $listen_socket->accept;
	if ($socket) {
		$socket->blocking(0);
		$socket->autoflush();
		my $connection = new_connection($socket);
		return $connection;
	}
	return undef;
}

sub new_connection($) {
	my ($socket) = @_;
	return {
		socket => $socket,
		parser => HTTP::Parser->new()
	};
}

# do the ssl handshake on the connection, and start listening for http requests when done.
# the handshake doesn't complete immediately, this sub calls itself again using Irssi::input_add until the handshake is done
sub accept_ssl {
	my ($connection) = @_;
	
	if ($connection->{watch_accepting_ssl_tag}) {
		Irssi::input_remove($connection->{watch_accepting_ssl_tag});
		delete $connection->{watch_accepting_ssl_tag};
	}
	
	my $rv = $connection->{socket}->accept_SSL();
	
	if ($rv) { # done
		connection_watch_read($connection);
	} else { # failed
		if ($SSL_ERROR == SSL_WANT_READ || $SSL_ERROR == SSL_WANT_WRITE) { # need to wait on read or write for ssl handshake
			$connection->{watch_accepting_ssl_tag} = Irssi::input_add(
				fileno($connection->{socket}),
				$SSL_ERROR == SSL_WANT_READ ? INPUT_READ : INPUT_WRITE,
				\&accept_ssl,
				$connection
			);
		} else {
			debug("SSL handshake failed");
		}
	}
}

sub connection_watch_read($) {
	my ($connection) = @_;
	$connection->{watch_read_tag} = Irssi::input_add(fileno($connection->{socket}), INPUT_READ, \&handle_can_read, $connection);
}

sub connection_unwatch_read($) {
	my ($connection) = @_;
	Irssi::input_remove($connection->{watch_read_tag});
	delete $connection->{watch_read_tag};
}

sub connection_watch_write($) {
	my ($connection) = @_;
	$connection->{watch_write_tag} = Irssi::input_add(fileno($connection->{socket}), INPUT_WRITE, \&handle_can_write, $connection);
}

sub connection_unwatch_write($) {
	my ($connection) = @_;
	Irssi::input_remove($connection->{watch_write_tag});
	delete $connection->{watch_write_tag};
}

# callback for Irssi::input_add when data can be read from a socket
sub handle_can_read($) {
	my ($connection) = @_;
	my $data;
	my $read = $connection->{socket}->read($data, BUFSIZ);
	
	if (defined($read)) {
		if ($read == 0) {
			debug("other end closed the connection");
			connection_close($connection);
		} else {
			handle_input($connection, $data);
		}
	} else {
		unless(defined($read)) {
			if ($! == EINTR || $! == EAGAIN || $! == EWOULDBLOCK) {
				return;
			} elsif ($! == ECONNRESET) {
				connection_unwatch_read($connection);
				debug("handle_can_read: connection closed");
				if (defined($connection->{output_buffer})) {
					# let the writing finish before closing the connection
					$connection->{close_when_write_ready} = 1;
				} else {
					connection_close($connection);
				}
			} else {
				print_warn ("Error reading from socket: $!");
				connection_close($connection);
			}
		}
	}
}

# write the data to the connection. Buffers data and adds watcher for when it can be written.
sub write_raw($$) {
	my ($connection, $data) = @_;
	if (defined($connection->{output_buffer})) {
		$connection->{output_buffer} .= $data;
	} else {
		$connection->{output_buffer} = $data;
		connection_watch_write($connection);
	}
}

# callback for Irssi::input_add when data can be written to a socket
sub handle_can_write {
	my ($connection) = @_;
	
	my $written =  $connection->{socket}->syswrite($connection->{output_buffer}, length($connection->{output_buffer}));
	if (defined($written)) { # if write successful
		if ($written == length($connection->{output_buffer})) { # everything has been written
			delete $connection->{output_buffer};
			connection_unwatch_write($connection);
			if ($connection->{close_when_write_ready}) {
				connection_close($connection);
			}
		} else {
			# remove what we wrote from output buffer
			substr($connection->{output_buffer}, 0, $written) = '';
		}
	} else { # if we couldn't write
		if ($! == EWOULDBLOCK || $! == EINTR || $! == EAGAIN) {
			return; # ok, ignore it
		}
		
		print_warn("handle_can_write: write error: $!");
		connection_close($connection);
	}
}

# closes the given connection, and cleans up watchers
sub connection_close($) {
	my ($connection) = @_;
	debug("closing connection $connection->{socket}");
	$connection->{socket}->close(SSL_no_shutdown => 1);
	delete $connections{$connection->{socket}};
	if ($connection->{watch_read_tag}) {
		connection_unwatch_read($connection);
	}
	if ($connection->{watch_write_tag}) {
		connection_unwatch_write($connection);
	}
}

########## HTTP ##########

sub handle_input($$) {
	my ($connection, $data) = @_;
	
	while (length($data) && $connection->{parser}->add($data) == 0) { # request completely parsed
		my $request = $connection->{parser}->request();
		#debug("handle_can_read: request: $request");
	
		handle_request($connection, $request);
	
		# start a new parser for the next request
		$connection->{parser} = HTTP::Parser->new();
		
		# push extra data that wasn't part of this request to the next parser
		$data = $connection->{parser}->data();
	}
}

sub handle_request($) {
	my ($connection, $request) = @_;
	my $client = $connection->{socket};
	#debug('Getting request from ' . $client);	

	my $url = $request->url->path;
	#debug('Request: method:' . $request->method . " url:\"$url\"");
	
	if (! check_request_authorization($request)) {
		send_authorization_required($connection);
	} elsif ($url eq '/events.json') {
		my ($sessionid, $events_ack, $commands) = split / /, $request->content(), 3;
		
		# session
		my $session;
		if ($sessionid eq 'newSession') {
			$session = new_session();
			$session->{client_ip} = $client->peerhost();
		} else {
			$session = $sessions{$sessionid};
			if (!$session) {
				my $response = HTTP::Response->new(200, undef, undef, "[{\"type\":\"unknown session\", \"i\": -1}]");
				print_warn("unknown session $sessionid");
				send_response_http($connection, $response);
				return;
			}
		}
		
		$session->{waiting_on_client_since} = undef; # not waiting anymore, we got one
		
		if ($session->{pending_connection}) {
			#debug("dropping older request");
			my $pending_connection = $session->{pending_connection};
			my $response = HTTP::Response->new(200, undef, undef, "[{\"type\":\"request superseded\", \"i\": -1}]");
			send_response_http($pending_connection, $response);
			delete $session->{pending_connection};
		}
		
		$session->{pending_connection} = $connection;
		
		events_ack($session, $events_ack);
		
		# commands
		processCommands($session, $commands);
		
		send_response_later($session);
	} elsif ($url =~ /^(\/[a-zA-Z0-9_]+)*\/[a-zA-Z0-9_\.]*$/ ) {
		if ($url eq '/') {
			$url = '/Webssi.html';
		}
		my $filename = Irssi::get_irssi_dir() . '/webssi' . $url;
		debug("Returning file $filename");
		send_file_response($connection, $filename);
	} else {
		debug('403 bad chars');
		my $response = HTTP::Response->new(403, undef, undef, 'URL contains characters that are not allowed.');
		send_response_http($connection, $response);
	}
}

sub send_file_response($$) {
	my ($connection, $filename) = @_;
	
	# all the files we're serving are relatively small. So just read it completely into memory and send it
	#local $/=undef; # locally clear $/ so <> reads the whole file at once
	if (!open(FILE, $filename)) {
		my $response = HTTP::Response->new(404, undef, undef, 'File not found');
		send_response_http($connection, $response);
		return;
	}
	binmode FILE;
	my $content = join("", <FILE>);
	close FILE;
	
	my $response = HTTP::Response->new(200, undef, undef, $content);
	send_response_http($connection, $response);
}

sub send_response_http($$) {
	my ($connection, $response) = @_;
	
	$response->protocol("HTTP/1.1");
	$response->header("Server" => "Webssi");
	my $content = $response->content();
	$response->header("Content-Length" => length($content));

	write_raw($connection, $response->as_string() . "\n");
}

sub send_response_later($) {
	my ($session) = @_;
	if ($session->{response_scheduled} || ! $session->{pending_connection} || scalar(@{$session->{events}}) == 0) {
		return;
	}
	$session->{response_scheduled} = 1;
	# note: not using the execute_later queue here, because commands in that queue should still be execute before this one
	Irssi::timeout_add_once(50, \&send_response_now, $session);
}

sub send_response_now($) {
	my ($session) = @_;
	
	my $connection = $session->{pending_connection};
	
	if(!$connection) {
		die "no connection to send response to";
	}
	
	before_send_response();
	
	# get events
	my $eventstring = get_events_as_json($session);
	
	# send reply
	my $response = HTTP::Response->new(200, undef, undef, $eventstring);
	#debug("sending: $eventstring");
	send_response_http($connection, $response);
	
	delete $session->{pending_connection};
	$session->{response_scheduled} = 0;
}

# for easier development only. There isn't any "normal"" way to enable this (just Irssi::Script::webssi::disable_authentication())
my $authentication_disabled = 0;
sub disable_authentication {
	$authentication_disabled = 1;
}

sub check_request_authorization($) {
	my ($request) = @_;
	if ($authentication_disabled) {
		return 1;
	}
	my ($request_username, $request_password) = $request->authorization_basic;
	if (! defined($request_username)) {
		debug("no authorization");
		return 0;
	}
	if ($request_password eq $settings_password) {
		#debug("Password ok");
		return 1;
	} else {
		debug("Password not ok");
		return 0;
	}
}

sub send_authorization_required($) {
	my ($connection) = @_;
	
	my $response = HTTP::Response->new(401, "Unauthorized", undef, "Unauthorized\n");
	$response->header('WWW-Authenticate' => 'Basic realm="Webssi"');
	send_response_http($connection, $response);
}

Irssi::signal_add('setup changed', \&refresh_servers);

refresh_servers(1);

sub UNLOAD {
	if ($http_listen_socket) {
		$http_listen_socket->close();
		$http_listen_socket = undef;
	}
	if ($https_listen_socket) {
		$https_listen_socket->close();
		$https_listen_socket = undef;
	}
}

########## EXECUTE QUEUE ##########
my @execute_queue;

# executes the given sub a little later, after currently processing events are done.
sub execute_later($) {
	my ($closure) = @_;
	if (scalar(@execute_queue) == 0) {
		Irssi::timeout_add_once(50, \&execute_queue_now, undef);
	}
	push @execute_queue, $closure;
}

sub execute_queue_now() {
	my @executing = @execute_queue;
	@execute_queue = ();
	for my $closure (@executing) {
		&$closure();
	}
}

########## EVENTS ##########

sub add_event($$) {
	my ($session, $event) = @_;
	
	return if ($session->{removed}); # session just got removed while something on the stack was still using it; just ignore it
	
	my %event_copy = %$event;
	$session->{'last_event_id'} = defined($session->{'last_event_id'}) ? ($session->{'last_event_id'} + 1) : 1;
	$event_copy{'i'} = $session->{'last_event_id'};
	push @{$session->{events}}, \%event_copy;
	
	if ($session->{waiting_on_client_since}) {
		if (time() - $session->{waiting_on_client_since} > 60) {
			remove_session($session);
			Irssi::print('Webssi: Session ' . $session->{'id'} . ' expired');
			return;
		}
	} elsif (! $session->{pending_client}) {
		$session->{waiting_on_client_since} = time();
	}
	
	send_response_later($session);
}

# adds event to all logged in sessions
sub add_event_all($) {
	my ($event) = @_;
	for my $session (sessions()) {
		add_event($session, $event);
	}
}

# adds event to every session following the given item
sub add_event_item_following($$) {
	my ($item, $event) = @_;
	for my $session (sessions()) {
		if (is_following_winitem($session, $item)) {
			add_event($session, $event);
		} else {
			my $winitem_state = get_winitem_state($session, $item);
			if ($winitem_state && $winitem_state->{uptodate}) {
				# not sending this event, so it is not up to date anymore
				#$winitem_state->{uptodate} = 0;
				# no point in remembering this winitem_state
				delete $session->{items}->{item_to_id($item)}; 
			}
		}
	}
}

# return the pending events for the given session encoded as json
sub get_events_as_json($) {
	my ($session) = @_;
	convert_utf8($session->{events});
	my $jsonstring = new JSON->ascii->encode($session->{events});
	return $jsonstring;
}

# remove events from the queue of the given session with ids smaller than or equal to $events_ack,
# because they have been received by the client
sub events_ack($$) {
	my ($session, $events_ack) = @_;
	my $events = $session->{events};
	while (scalar(@$events) != 0 && $events->[0]->{'i'} <= $events_ack) {
		shift @$events;
	}
}

# this is an ugly hack
# calls utf8::decode on all strings in the given data structure of arrays and hashes 
sub convert_utf8 {
	my ($o) = @_;
	if (ref($o) eq 'ARRAY') {
		for (my $i; $i < scalar(@$o); $i++) {
			my $v = $o->[$i];
			if (ref($v)) {
				convert_utf8($o->[$i]);
			} elsif ($v !~ /^\d*$/) { # don't touch numbers
				utf8::decode($o->[$i]);
			}
		}
	} elsif (ref($o) eq 'HASH') {
		foreach my $key (keys(%$o)) {
			my $v = $o->{$key};
			if (ref($v)) {
				convert_utf8($o->{$key});
			} elsif ($v !~ /^\d*$/) { # don't touch numbers
				utf8::decode($o->{$key});
			}
		}
	}
}

########## CREATE EVENTS ##########

sub ev($$) {
	my ($type, $args) = @_;
	$args->{type} = $type;
	return $args;
}

sub ev_window($$$) {
	my ($type, $win, $args) = @_;
	my $result = ev($type, $args);
	$result->{window} = window_to_id($win);
	return $result;
}

sub ev_window_new($) {
	my ($window) = @_;
	return ev_window('window new', $window, {
		name => $window->{'name'},
		refnum => $window->{'refnum'},
		data_level => $window->{data_level},
		hilight_color => $window->{hilight_color}
	});
}

# window is passed here as an extra argument because $item->window() isn't always what we want it to be
# when item is being moved, removed, or is null
sub ev_window_item($$$$) {
	my ($type, $window, $item, $args) = @_;
	my $result = ev_window($type, $window, $args);
	$result->{item} = item_to_id($item);
	if ($item->{server}) {
		$result->{tag} = $item->{server}->{tag};
	}
	return $result;
}

sub ev_channel($$$) {
	my ($type, $channel, $args) = @_;
	return ev_window_item($type, $channel->window(), $channel, $args);
}

sub ev_text($$) {
	my ($win, $text) = @_;
	return ev_window('T', $win, {text => $text});
}

sub ev_nicklist($$$$) {
	my ($type, $channel, $nick, $args) = @_;
	my $result = ev_channel($type, $channel, $args);
	$result->{name} = $nick->{nick};
	return $result;
}

sub ev_nicklist_new($$) {
	my ($channel, $nick) = @_;
	return ev_nicklist('nicklist new', $channel, $nick, {});
}

sub ev_window_item_new($$) {
	my ($win, $item) = @_;
	my $result = ev_window_item('window item new', $win, $item, {
		visible_name => $item->{visible_name},
		is_active => $item->is_active(),
		data_level => $item->{data_level},
		hilight_color => $item->{hilight_color}
	});
	if ($item->isa('Irssi::Channel')) {
		$result->{'item_type'} = 'channel';
	}
	return $result;
}

sub ev_server($$$) {
	my ($type, $server, $args) = @_;
	my $result = ev($type, $args);
	$result->{tag} = $server->{tag};
	return $result;
}

sub ev_server_new($) {
	my ($server) = @_;
	return ev_server('server new', $server, {});
}

########## OBJECT ID MAPPING ##########

sub window_to_id($) {
	my ($window) = @_;
	return '' . $window->{'_irssi'};
}

sub id_to_window($) {
	my ($id) = @_;
	return (grep {$_->{'_irssi'} eq $id} Irssi::windows())[0];
}

sub item_to_id($) {
	my ($item) = @_;
	if (! defined($item)) {
		return undef;
	}
	my $tag = $item->{server} ? $item->{server}->{tag} . ' ': 'itemWithoutServer';
	return ($tag . $item->{name});
}

sub id_to_item($) {
	my ($id) = @_;
	my ($tag, $name) = split(/ /, $id);
	if (defined($name)) {
		my $server = Irssi::server_find_tag($tag);
		return $server ? $server->window_item_find($name) : undef;
	} else {
		return Irssi::window_item_find($id);
	}
}

########## INIT AND RESYNC EVENTS ##########

sub add_init_events($) {
	my ($session) = @_;
	add_event($session, ev('init', {sessionid => $session->{id}}));
	foreach my $server (Irssi::servers()) {
		add_event($session, ev_server_new($server));
	}
	foreach my $win (Irssi::windows()) {
		add_event($session, ev_window_new($win));
		foreach my $item ($win->items()) {
			add_event($session, ev_window_item_new($win, $item));
		}
		update_following_window($win);
	}
	add_event($session, ev('window changed', {'window' => window_to_id(Irssi::active_win())}));
}

# bring the given session up-to-date on the given window.
# called when the session starts following the window (again)
sub resync_window($$) {
	my ($session, $win) = @_;
	update_text_later($session, $win);
}

# re-send the full details (nicklist) of the item to the session
sub resync_winitem($$) {
	my ($session, $item) = @_;
	if ($item->isa('Irssi::Channel')) {
		add_event($session, ev_channel('nicklist clear', $item, {}));
		foreach my $nick ($item->nicks()) {
			add_event($session, ev_nicklist_new($item, $nick));
		}
	}
}

#sub get_win_state($$) {
#	my ($session, $win) = @_;
#	return $session->{windows}->{window_to_id($win)};
#}
#
#sub get_or_create_win_state($$) {
#	my ($session, $win) = @_;
#	my $win_state = $session->{windows}->{window_to_id($win)};
#	if (!$win_state) {
#		$win_state = {};
#		$session->{windows}->{window_to_id($win)} = $win_state;
#	}
#	return $win_state;
#}

sub get_winitem_state($$) {
	my ($session, $winitem) = @_;
	return $session->{items}->{item_to_id($winitem)};
}

# returns the state of the given item in the session, or creates a new state if none was found
sub get_or_create_winitem_state($$) {
	my ($session, $winitem) = @_;
	my $winitem_state = $session->{items}->{item_to_id($winitem)};
	if (!$winitem_state) {
		$winitem_state = {};
		$session->{items}->{item_to_id($winitem)} = $winitem_state;
	}
	return $winitem_state;
}

sub is_following_window($$) {
	my ($session, $win) = @_;
	return Irssi::active_win()->{refnum} == $win->{refnum};
}

sub is_following_winitem($$) {
	my ($session, $winitem) = @_;
	return is_following_window($session, $winitem->window()); # follow all items in a followed window
}

# make sure every client following this window (or items in it) is up-to-date
sub update_following_window($) {
	my ($win) = @_;
	for my $session (sessions()) {
		if (is_following_window($session, $win)) {
			resync_window($session, $win);
		}
	}
	for my $item ($win->items()) {
		update_following_winitem($item);
	}
}

# make sure every client following this window item is up-to-date
sub update_following_winitem($) {
	my ($winitem) = @_;
	for my $session (sessions()) {
		if (is_following_winitem($session, $winitem)) {
			my $winitem_state = get_or_create_winitem_state($session, $winitem);
			if (! $winitem_state->{uptodate}) {
				resync_winitem($session, $winitem);
				$winitem_state->{uptodate} = 1;
			}
		}
	}
}

########## SIGNALS ##########

Irssi::signal_add('window created', sub {
	my ($win) = @_;
	add_event_all(ev_window_new($win));
	update_following_window($win);
});

Irssi::signal_add('window destroyed', sub {
	my ($win) = @_;
	add_event_all(ev('window remove', {window => window_to_id($win)}));
	
#	# remove state of window from all sessions
#	for my $session (sessions()) {
#		if (get_win_state($session, $win)) {
#			delete $session->{windows}->{window_to_id($win)};
#		}
#	}
});

Irssi::signal_add('window changed', sub {
	my ($win, $old) = @_;
	add_event_all(ev('window changed', {window => window_to_id($win), old => window_to_id($old)}));
	for my $session (sessions()) {
		update_following_window($win);
		if ($old) {
			update_following_window($old);
		}
	}
});

Irssi::signal_add('window item new', sub {
	my ($win, $item) = @_;
	add_event_all(ev_window_item_new($win,$item));
	update_following_winitem($item);
});

Irssi::signal_add('window item remove', sub {
	my ($window, $item) = @_;
	add_event_all(ev_window_item('window item remove', $window, $item, {}));
	
	# remove state of item from all sessions
	for my $session (sessions()) {
		if (get_winitem_state($session, $item)) {
			delete $session->{items}->{item_to_id($item)};
		}
	}
});

Irssi::signal_add('window item moved', sub {
	my ($window, $item, $old_window) = @_;
	add_event_all(ev_window_item('window item moved', $old_window, $item, {new_window_event => ev_window(undef, $window, {})}));
	update_following_winitem($item);
});

Irssi::signal_add('window item changed', sub {
	my ($window, $item) = @_;
	# note: $item might be null here
	add_event_all(ev_window('window item changed', $window, {item => item_to_id($item)}));
	update_following_window($window);
});

Irssi::signal_add('window item activity', sub {
	my ($item, $old_level) = @_;
	if ($item->{data_level} != $old_level) {
		add_event_all(ev_window_item('window item activity', $item->window(), $item, {data_level => $item->{data_level}, hilight_color => $item->{hilight_color}}));
	}
});

Irssi::signal_add('nicklist new', sub {
	my ($channel, $nick) = @_;
	add_event_item_following($channel, ev_nicklist_new($channel, $nick));
});

Irssi::signal_add('nicklist remove', sub {
	my ($channel, $nick) = @_;
	if ($channel->window()) { # ignore events for a channel that's just been removed
		add_event_item_following($channel, ev_nicklist('nicklist remove', $channel, $nick, {}));
	}
});

Irssi::signal_add('nicklist changed', sub {
	my ($channel, $nick, $old_name) = @_;
	# can't use ev_nicklist here because the nick $nick has already changed.
	# we send the event with the old nick, passing the new one as extra arg
	add_event_item_following($channel, ev_channel('nicklist changed', $channel, {
		name => $old_name,
		new_name => $nick->{nick}
	}));
});

Irssi::signal_add('window refnum changed', sub {
	my ($window, $old) = @_;
	add_event_all(ev('window refnum changed', {'window' => window_to_id($window), 'refnum' => $window->{'refnum'}}));
});

Irssi::signal_add('window name changed', sub {
	my ($window, $old) = @_;
	add_event_all(ev('window name changed', {'window' => window_to_id($window), 'name' => $window->{'name'}}));
});

Irssi::signal_add('window activity', sub {
	my ($window, $old_level) = @_;
	if ($window->{data_level} != $old_level) {
		add_event_all(ev_window('window activity', $window, {data_level => $window->{data_level}, hilight_color => $window->{hilight_color}}));
	}
});

Irssi::signal_add('server connected', sub {
	my ($server) = @_;
	add_event_all(ev('server new', {tag => $server->{tag}}));
});

Irssi::signal_add('server disconnected', sub {
	my ($server) = @_;
	add_event_all(ev('server remove', {tag => $server->{tag}}));
});

# Catch 'print text' as late as possible (Irssi::SIGNAL_PRIORITY_LOW == 100)
Irssi::signal_add_priority('print text', \&sig_print_text, 200);
# set to a positive number if lines being printed should be ignored (to prevent infinite recursion on errors or debug messages) 
my $ignore_printing = 0;

sub sig_print_text {
	my ($dest, $text, $stripped) = @_;
	
	if ($ignore_printing) {
		return;
	}
	$ignore_printing++;
	
	for my $session (sessions()) {
		if (is_following_window($session, $dest->{window})) {
			update_text_later($session, $dest->{window});
		}
	}
#	#add_event_all(ev_text($dest->{'window'}, text_to_html($text)));
	
	$ignore_printing--;
}

# Add events for new text in the given window to the client "soon".
# The small delay is added to push as much text into one event as possible
sub update_text_later {
	my ($session, $window) = @_;
	execute_later(sub {
		update_text_now($session, $window);
	});
}

# add events for new text in the given window to the clients
sub update_text_now {
	my ($session, $window) = @_;
	
	# if in the short time we delayed before actually updating the window,
	# we already stopped following it (for example /eval window next;window next)
	if (! is_following_window($session, $window)) {
		return; # don't update
	}
	
	my $html = '';

	my $view = $window->view;
	
	my $line;
	my $last_sent_line = $view->get_bookmark('webssi_' . $session->{id});
	if ($last_sent_line) {
		$line = $last_sent_line->next;
	} else {
		$line = $view->get_lines;
	}
	
	while ($line) {
		my $text = $line->get_text(1);
		$html .= text_to_html($text);
		$line = $line->next;
	}
	
	if ($html ne '') {
		add_event($session, ev_text($window, $html));
		$view->set_bookmark_bottom('webssi_' . $session->{id});
	}
}

# remove all bookmarks added for the session
# called when session is removed to avoid leaking
sub clear_session_bookmarks($) {
	my ($session) = @_;
	foreach my $win (Irssi::windows()) {
		if ($win->view->get_bookmark('webssi_' . $session->{id})) {
			$win->view->set_bookmark('webssi_' . $session->{id}, undef);
		}
	}
}

#Irssi::signal_add('gui print text', \&sig_gui_print_text);
#sub sig_gui_print_text {
#	my ($win, $fg, $bg, $flags, $text, $dest) = @_;
#	if ($ignore_printing || $text =~ /DEBUG/) {
#		return;
#	}
#	$ignore_printing++;
#	add_event_all({ type => 'T', text => $text});
#	$ignore_printing--;
#}

my $last_entry;
my $last_entry_pos;

sub update_entry {
	my $entry = Irssi::parse_special('$L');
	my $entry_pos = Irssi::gui_input_get_pos();
	return if (defined($last_entry) && $entry eq $last_entry && $last_entry_pos == $entry_pos);
	add_event_all(ev('entry changed', {content => $entry, cursorPos => $entry_pos }));
	$last_entry = $entry;
	$last_entry_pos = $entry_pos;
	#debug("entry: " . $entry);
}

Irssi::signal_add_last('gui key pressed', sub {
	update_entry();
	#debug("key:" . $_[0])
});

sub before_send_response() {
	execute_queue_now(); # make sure the queue is empty before we're sending a response
	update_entry();
}

########## WEBSSI COMMANDS ##########

sub processCommands($$) {
	my ($session, $commandstring) = @_;
	return unless defined($commandstring);
	my $commands = decode_json($commandstring);
	if (ref $commands ne 'ARRAY') {
		debug("ERROR: $commands isn't an array");
		return;
	}
	
	return unless (scalar(@$commands)); # return if there are no events
	
	foreach my $command (@$commands) {
		if (ref $command ne 'HASH') {
			debug("ERROR: $command isn't a hash");
			return;
		}
		
		if (defined($session->{'last_processed_command'}) && $command->{'id'} <= $session->{'last_processed_command'}) {
			next;
		}
		
		$session->{'last_processed_command'} = $command->{'id'};
		
		# signal that we start processing the command
		add_event($session, ev('command', {'id' => $command->{'id'}}));

		if ($command->{'type'} eq 'sendLine') {
			my $win = id_to_window($command->{'win'});
			my $line = $command->{'line'};
			cmd_send_line($win, $line);
		} elsif ($command->{type} eq 'activateWindow') {
			my $win = id_to_window($command->{'win'});
			cmd_activate_window($win);
		} elsif ($command->{type} eq 'activateWindowItem') {
			my $item = id_to_item($command->{'item'});
			cmd_activate_window_item($item);
		} elsif ($command->{type} eq 'key') {
			my $keys = $command->{'keys'};
			cmd_key($keys);
		}
	}
	
	# not processing any command anymore
	add_event($session, ev('command', {'id' => undef}));
}

sub cmd_send_line($$) {
	my ($win, $line) = @_;
	my $command = ($line =~ /^\//) ? $line : "MSG * $line";
	if (! $win) {
		$win = Irssi::active_win();
	}
	
	$win->command($command);
}

sub cmd_activate_window($) {
	my ($win) = @_;
	$win->set_active();
}

sub cmd_activate_window_item($) {
	my ($item) = @_;
	$item->set_active();
}

sub cmd_key($) {
	my ($keys) = @_;
	foreach my $key (@$keys) {
		Irssi::signal_emit('gui key pressed', $key)
	}
}

########## IRSSI COMMANDS ##########

Irssi::command_bind('webssi', sub {
    my ($data, $server, $item) = @_;
    $data =~ s/\s+$//g;
    Irssi::command_runsub('webssi', $data, $server, $item);
});

Irssi::command_bind('webssi reset', sub {
	foreach my $session (sessions()) {
		remove_session($session);
	}
	refresh_servers(1);
});

Irssi::command_bind('webssi status', sub {
	Irssi::print("Webssi status:");
	if ($http_listen_socket) {
		Irssi::print(" HTTP server listening on $settings_http_port");
	}
	if ($https_listen_socket) {
		Irssi::print(" HTTPS server listening on $settings_https_port");
	}
	if (!$http_listen_socket && !$https_listen_socket) {
		Irssi::print(' Not listening on any port.');
	}
	if (keys(%sessions)) {
		Irssi::print(' Sessions:');
		for my $session (sessions()) {
			my $waiting = $session->{waiting_on_client_since} ? (time() - $session->{waiting_on_client_since}) : 0;
			Irssi::print('  From ' . $session->{client_ip} . ' started ' . (time() - $session->{creation_time}). ' sec ago'
				. ($waiting ? (', waiting on client since '. $waiting . ' sec ago') : '')
				#. ', ' . scalar(@{$session->{events}}) . " events pending" # TODO check before starting output
			);
		}
	} else {
		Irssi::print(" No active sessions");
	}
	
});

########## DEBUG ##########

my $debug_enabled = 0;

# for development only (call using Irssi::Script::webssi::enable_debug())
sub enable_debug {
	$debug_enabled = 1;
}

sub debug {
	if ($debug_enabled) {
		$ignore_printing++;
		Irssi::print("DEBUG: " . $_[0]);
		$ignore_printing--;
	}
}

sub print_warn {
	$ignore_printing++;
	Irssi::print("WARN: " . $_[0]);
	$ignore_printing--;
}

########## COLORS ##########
# based on log2ansi.pl by Peder Stray

my (%attr, %old);

#my @bols = qw(bold underline blink reverse fgh bgh);
#my @nums = qw(fgc bgc);

my $n = 0;
my %ansi = map { $_ => $n++ } split //, 'krgybmcw';
my @irssi = split //, 'kbgcrmywKBGCRMYW';

my @webssi_fg = qw(fk fb fg fc fr fm fy fw lK lB lG lC lR lM lY lW);
my @webssi_bg = qw(bk bb bg bc br bm by bw oK oB oG oC oR oM oY oW);

my @ic = map {$ansi{lc $_}} @irssi;
my @ih = map {$_ eq uc $_} @irssi;

sub defc {
    my($attr) = shift || \%attr;
    #$attr->{fgc} = $attr->{bgc} = -1;
    #$attr->{fgh} = $attr->{bgh} = 0;
    $attr->{fg} = $attr->{bg} = -1;
}

sub defm {
    my($attr) = shift || \%attr;
    $attr->{bold} = $attr->{underline} = 
      $attr->{blink} = $attr->{reverse} = $attr->{monospace} = 0;
}

sub def {
    my($attr) = shift || \%attr;
    defc($attr);
    defm($attr);
}

sub setold {
    %old = %attr;
}

my $buffer;

my %html_escaped = (
	'<' => '&lt;',
	'>' => '&gt;',
	'&' => '&amp;'
);

sub emit($) {
	my ($text) = @_;
	
	# escape special chars
	$text =~ s/([<>&])/$html_escaped{$1}/ge;
	
	my $class = '';
	$class .= 'b ' if $attr{bold};
	$class .= 'u ' if $attr{underline};
	$class .= 'm ' if $attr{monospace};
	#$class .= 'l ' if $attr->{blink};
	#$class .= 'r ' if $attr->{reverse};
	my $fg = $attr{fg};
	my $bg = $attr{bg};
	if ($attr{reverse}) {
		my $tmp = $bg;
		$bg = $fg;
		$fg = $tmp;
	}
	if (defined($fg) && $fg != -1) {
		$class .= $webssi_fg[$fg] . ' ';
	}
	if (defined($bg) && $bg != -1) {
		$class .= $webssi_bg[$bg] . ' ';
	}
	
	#$buffer .= '</span>';
	if ($class ne '') {
		#debug("class='$class' fg='$fg' bg='$bg' text='$text'");
		chop $class; # chop space
		$buffer .= "<span class='$class'>";
		$buffer .= $text;
		$buffer .= '</span>';
	} else {
		$buffer .= $text;
	}
}

sub text_to_html($) {
    ($_) = @_;
    def;
    setold;
    $buffer = '';
    my $buffer_has_indent = 0;
    my $prefix = '<div>';
    my $suffix = '</div>';

    while (length) {
	if (s/^\cD//) {
	    if (s/^a//) {
		$attr{blink} = !$attr{blink};
	    } elsif (s/^b//) {
		$attr{underline} = !$attr{underline};
	    } elsif (s/^c//) {
		$attr{bold} = !$attr{bold};
	    } elsif (s/^d//) {
		$attr{reverse} = !$attr{reverse};
	    } elsif (s/^e//) {
		if (!$buffer_has_indent) {
			$buffer_has_indent = 1;
			$prefix .= "<div class='p3'><div class='p1'>";
			$buffer .= "</div><div class='p2'>";
			$suffix = "</div></div>" . $suffix;
		}
	    } elsif (s/^f([^,]*),//) {
		# indent_func
	    } elsif (s/^g//) {
		def;
	    } elsif (s/^h//) {
		# clrtoeol
	    } elsif (s/^i//) {
		$attr{monospace} = ! $attr{monospace};
		debug('monospace');
	    #} elsif (s/^([\/-?])([\/-?])//) {
	    } elsif (s/^(.)(.)//) {
		$attr{fg} = (ord($1) == 255 || $1 eq '/') ? -1 : ((ord($1)-ord('0')) % 16);
		$attr{bg} = (ord($2) == 255 || $2 eq '/') ? -1 : ((ord($2)-ord('0')) % 16);
	    }
	} elsif (s/^\c_//) {
	    $attr{underline} = !$attr{underline};
	} elsif (s/^\cV//) {
	    $attr{reverse} = !$attr{reverse};
	} else {
	    #s/^(.[^\cB\cC\cD\cF\cO\cV\c[\c_]*)//;
	    s/^(.[^\cD\c_\cV]*)//;
	    emit $1;
	}
    }

    return $prefix . $buffer . $suffix;
}
