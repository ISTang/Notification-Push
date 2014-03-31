var passport = require('passport')
  , LocalStrategy = require('passport-local').Strategy
  , ensureLoggedIn = require('connect-ensure-login').ensureLoggedIn;
var user = require('connect-roles');

var express = require('express');
var app = express();

app.configure(function () {
    app.engine('.html', require('ejs').__express);
    app.set('views', __dirname + '/views');
    app.set('view engine', 'html');

   app.use(express.static('public'));
   app.use(express.cookieParser());
   app.use(express.bodyParser());
   
   app.use(express.session({ secret: 'keyboard cat' }));
   app.use(passport.initialize());
   app.use(passport.session());
   
   app.use(app.router);
});

app.post('/login',
  passport.authenticate('local', { successRedirect: '/',
                                   failureRedirect: '/login',
                                   failureFlash: false })
);

app.use(user);

passport.use(new LocalStrategy(
  function(username, password, done) {
      if (username!='user' && username!='admin') {
        return done(null, false, { message: 'Incorrect username.' });
      }
      if (password!='pass') {
        return done(null, false, { message: 'Incorrect password.' });
      }
      var user = {name:username};
      if (username='user') user.role = 'user';
      else user.role = 'admin';
      return done(null, user);
  }
));

passport.serializeUser(function(user, done) {
  done(null, user.name);
});

passport.deserializeUser(function(name, done) {
  done(null, {name:name, role:name});
});

user.use('access private page', function(req) {
  if (req.user.role === 'user') {
    return true;
  }
});

user.use(function(req) {
  if (req.user.role === 'admin') {
    return true;
  }
});

//optionally controll the access denid page displayed
user.setFailureHandler(function (req, res, action){
  var accept = req.headers.accept || '';
  res.status(403);
  if (~accept.indexOf('html')) {
    res.render('access-denied', {action: action});
  } else {
    res.send('Access Denied - You don\'t have permission to: ' + action);
  }
});

app.get('/login', function (req, res) {
  res.render('login');
});

app.get('/', ensureLoggedIn('/login'), function (req, res) {
  res.render('home');
});
app.get('/private', ensureLoggedIn('/login'), user.can('access private page'), function (req, res) {
  res.render('private');
});
app.get('/admin', ensureLoggedIn('/login'), user.can('access admin page'), function (req, res) {
  res.render('admin');
});

app.listen(3000);
