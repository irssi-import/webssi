use strict;
use JSON;
use HTTP::Daemon::SSL;
use HTTP::Daemon;
use HTTP::Response;
use Data::Dumper;

use Irssi;
#use Irssi::TextUI;

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

########## HTTP ##########

Irssi::settings_add_str('webssi', 'webssi_password', '');
Irssi::settings_add_int('webssi', 'webssi_http_port', -1);
Irssi::settings_add_int('webssi', 'webssi_https_port', -1);
Irssi::settings_add_str('webssi', 'webssi_https_key_file', Irssi::get_irssi_dir()."/webssi-key.pem");
Irssi::settings_add_str('webssi', 'webssi_https_cert_file', Irssi::get_irssi_dir()."/webssi-cert.pem");

my ($settings_http_port, $settings_https_port, $settings_https_key_file, $settings_https_cert_file, $settings_password);
my ($http_daemon, $https_daemon);

# Called when loading, or when the settings have changed
# (re)starts the http and/or https daemon if forced or if relevant settings have changed
sub refresh_daemon {
	my ($force) = @_;
	
	if ($force || $settings_password ne Irssi::settings_get_str('webssi_password')) {
		if (Irssi::settings_get_str('webssi_password') eq '') {
			# if the password previously wasn't set, force start of daemon even when other settings haven't changed
			$force = 1;
		}
		$settings_password = Irssi::settings_get_str('webssi_password');
		if ($settings_password eq '') {
			Irssi::print("No password configured for webssi. Use /SET webssi_password <password>");
		}
	}
	
	if ($force || $settings_http_port != Irssi::settings_get_int('webssi_http_port')) {
		if ($http_daemon) {
			Irssi::print('Stopping listening on ' . $http_daemon->url);
			$http_daemon->close();
		}
		
		$settings_http_port = Irssi::settings_get_int('webssi_http_port');
		
		if ($settings_http_port > 0 && $settings_password ne '') {
			$http_daemon = HTTP::Daemon->new(
				LocalPort => $settings_http_port,
				Reuse => 1,
				Blocking => 0,
				LocalAddr => "127.0.0.1"
			);
			if (! $http_daemon) {
				Irssi::print("Failed to start HTTP server: " . $!);
			} else {
				Irssi::print('Webssi listening at: ' . $http_daemon->url . ' (for security reasons http can only be used from localhost)');
				Irssi::input_add(fileno($http_daemon), INPUT_READ, \&handle_http_connection, 0);
			}
		}
	}

	if ($force || $settings_https_port != Irssi::settings_get_int('webssi_https_port')
			|| $settings_https_key_file ne Irssi::settings_get_str('webssi_https_key_file')
			|| $settings_https_cert_file ne Irssi::settings_get_str('webssi_https_cert_file')) {
		if ($https_daemon) {
			Irssi::print('Stopping listening on ' . $https_daemon->url);
			$https_daemon->close();
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
				$https_daemon = HTTP::Daemon::SSL->new(
					LocalPort => $settings_https_port,
					Reuse => 1,
					Blocking => 0,
					SSL_key_file => $settings_https_key_file,
					SSL_cert_file => $settings_https_cert_file
				);
				if (! $https_daemon) {
					Irssi::print("Failed to start HTTPS server: " . $!);
				} else {
					Irssi::print('Webssi listening at: ' . $https_daemon->url);
					Irssi::input_add(fileno($https_daemon), INPUT_READ, \&handle_https_connection, 0);
				}
			}
		}
	}
	
	if ($settings_http_port <= 0 && $settings_https_port <= 0) {
		Irssi::print("No listening port configured for webssi. Use /SET webssi_https_port <port number>");
	}
}

my %tag;

sub handle_http_connection {
	handle_connection($http_daemon);
}

sub handle_https_connection {
	handle_connection($https_daemon);
}

sub handle_connection($) {
	my ($daemon) = @_;
	my $client = $daemon->accept;
	if ($client) {
		$tag{$client} = Irssi::input_add(fileno($client), INPUT_READ, \&handle_request, $client);
	}
}

sub handle_request($) {
	my ($client) = @_;
	#debug('Getting request from ' . $client);	
	my $request = $client->get_request;
	if (!$request) {
		#debug("closing connection to $client");
		# TODO if it's a pending_client of a connection, abort that one
		Irssi::input_remove($tag{$client});
		$client->close;
		delete $tag{$client};
		return;
	}

	my $url = $request->url->path;
	#debug('Request: method:' . $request->method . " url:\"$url\"");
	
	if (! check_request_authorization($request)) {
		send_authorization_required($client);
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
				$client->send_response($response);
				return;
			}
		}
		
		$session->{waiting_on_client_since} = undef; # not waiting anymore, we got one
		
		if ($session->{pending_client}) {
			#debug("dropping older request");
			my $client = $session->{pending_client};
			my $response = HTTP::Response->new(200, undef, undef, "[{\"type\":\"request superseded\", \"i\": -1}]");
			$client->send_response($response);
			delete $session->{pending_client};
		}
		
		$session->{pending_client} = $client;
		
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
		$client->send_file_response($filename);
	} else {
		debug('403 bad chars');
		my $response = HTTP::Response->new(403, undef, undef, 'URL contains characters that are not allowed.');
		$client->send_response($response);
	}
}

sub send_response_later($) {
	my ($session) = @_;
	if ($session->{response_scheduled} || ! $session->{pending_client} || scalar(@{$session->{events}}) == 0) {
		return;
	}
	$session->{response_scheduled} = 1;
	# note: not using the execute_later queue here, because commands in that queue should still be execute before this one
	Irssi::timeout_add_once(50, \&send_response_now, $session);
}

sub send_response_now($) {
	my ($session) = @_;
	
	my $client = $session->{pending_client};
	
	if(!$client) {
		die "no connection to send response to";
	}
	
	before_send_response();
	
	# get events
	my $eventstring = get_events_as_json($session);
	
	# send reply
	my $response = HTTP::Response->new(200, undef, undef, $eventstring);
	#debug("sending: $eventstring");
	$client->send_response($response);
	
	delete $session->{pending_client};
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
		debug("Password ok");
		return 1;
	} else {
		debug("Password not ok");
		return 0;
	}
}

sub send_authorization_required($) {
	my ($client) = @_;
	
	my $response = HTTP::Response->new(401, "Unauthorized", undef, "Unauthorized\n");
	$response->header('WWW-Authenticate' => 'Basic realm="Webssi"');
	$client->send_response($response);
}

Irssi::signal_add('setup changed', \&refresh_daemon);

refresh_daemon(1);

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
	refresh_daemon(1);
});

Irssi::command_bind('webssi status', sub {
	Irssi::print("Webssi status:");
	my @daemons;
	if ($http_daemon || $https_daemon) {
		if ($http_daemon) {
			push @daemons, $http_daemon->url;
		}
		if ($https_daemon) {
			push @daemons, $https_daemon->url;
		}

		Irssi::print(' Listening on ' . (join ' and ', @daemons));
	} else {
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
