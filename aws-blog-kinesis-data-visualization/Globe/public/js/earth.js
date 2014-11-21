(function() {
	
	// converts a point at (lat, lng) and a given altitue above the earth
	// to a point in 3d space.  (0,0,0) is the centre of the earth and
	// an altitude of 1 is the surface of the earth
	function getPosition(lat, lng, alt){
		var phi = (90 - lat) * Math.PI / 180;
		var theta = (17 - lng) * Math.PI / 180;
		
		var pos = {};
		pos.x = alt * Math.sin(phi) * Math.cos(theta);
		pos.y = alt * Math.cos(phi);
		pos.z = alt * Math.sin(phi) * Math.sin(theta);
		
		return pos;
	}
	
	function createScene(){
		// create the scene
		var scene = new THREE.Scene();		
		scene.add(new THREE.AmbientLight(0x333333));

		return scene
	}
	
	function createEarth(radius, segments) {
		return new THREE.Mesh(new THREE.SphereGeometry(radius, segments,
				segments), new THREE.MeshPhongMaterial({
			map : THREE.ImageUtils.loadTexture('images/2_no_clouds_4k.jpg'),
			bumpMap : THREE.ImageUtils.loadTexture('images/elev_bump_4k.jpg'),
			bumpScale : 0.01,
			specularMap : THREE.ImageUtils.loadTexture('images/water_4k.png'),
			specular : new THREE.Color(0x272727)
		}));
	}
	
	function createClouds(radius, segments) {
		return new THREE.Mesh(new THREE.SphereGeometry(radius + 0.006,
				segments, segments), new THREE.MeshPhongMaterial({
			map : THREE.ImageUtils.loadTexture('images/fair_clouds_4k.png'),
			transparent : true
		}));
	}
	
	function createStars(radius, segments) {
		return new THREE.Mesh(new THREE.SphereGeometry(radius, segments,
				segments), new THREE.MeshBasicMaterial({
			map : THREE.ImageUtils.loadTexture('images/galaxy_starfield.png'),
			side : THREE.BackSide
		}));
	}
	
	function createSun( radius ) {
		
		var light = new THREE.PointLight( 0xffffff, radius, radius );
		light.color.setHSL( 0.55, 0.9, 0.9 );
		
		var pos = getPosition(10, 0, radius);
		
		light.position.x = pos.x;
		light.position.y = pos.y;
		light.position.z = pos.z;
		
		return light;
	}
	
	function createLensFlare(sun){
		var textureFlare0 = THREE.ImageUtils.loadTexture( "images/lensflare0.png" );
		var textureFlare2 = THREE.ImageUtils.loadTexture( "images/lensflare2.png" );
		var textureFlare3 = THREE.ImageUtils.loadTexture( "images/lensflare3.png" );
		
		var flareColor = new THREE.Color( 0xffffff );
		flareColor.setHSL( 0.55, 0.9, 1.4 );
	
		var lensFlare = new THREE.LensFlare( textureFlare0, 400, 0.0, THREE.AdditiveBlending, flareColor );
	
		lensFlare.add( textureFlare2, 512, 0.0, THREE.AdditiveBlending );
		lensFlare.add( textureFlare2, 512, 0.0, THREE.AdditiveBlending );
		lensFlare.add( textureFlare2, 512, 0.0, THREE.AdditiveBlending );
	
		lensFlare.add( textureFlare3, 60, 0.6, THREE.AdditiveBlending );
		lensFlare.add( textureFlare3, 70, 0.7, THREE.AdditiveBlending );
		lensFlare.add( textureFlare3, 120, 0.9, THREE.AdditiveBlending );
		lensFlare.add( textureFlare3, 70, 1.0, THREE.AdditiveBlending );
	
		lensFlare.customUpdateCallback = lensFlareUpdateCallback;
		lensFlare.position.copy( sun.position );
	
		return lensFlare

	}

	function lensFlareUpdateCallback( object ) {
		var f, fl = object.lensFlares.length;
		var flare;
		var vecX = -object.positionScreen.x * 2;
		var vecY = -object.positionScreen.y * 2;
	
		for( f = 0; f < fl; f++ ) {
	
			   flare = object.lensFlares[ f ];
	
			   flare.x = object.positionScreen.x + vecX * flare.distance;
			   flare.y = object.positionScreen.y + vecY * flare.distance;
	
			   flare.rotation = 0;
	
		}
		object.lensFlares[ 2 ].y += 0.025;
		object.lensFlares[ 3 ].rotation = object.positionScreen.x * 0.5 + THREE.Math.degToRad( 45 );	
	}
	
	function addRandomLights(){
		var lights = [];
		for(var i = 0; i < 50; i++) {
			var lght = addLight(radius, (Math.random() * 110) - 55, (Math.random() * 360) - 180);
			lights.push(lght);
		}
		
		return lights;
	}
	
	function addLight(radius, lat, lng) {
		var l = new THREE.PointLight(0x6DAEE1, 0, 0.25);
		
		var pos = getPosition(lat, lng, radius + 0.05);

		l.position.x = pos.x;
		l.position.y = pos.y;
		l.position.z = pos.z;

		l.inUse = false;
		
		scene.add(l);
	
		return l;
	}
	
	function startLight(lat, lng, lifeTime, color) {
		for(var i = 0; i < lights.length; i++){
			var light = lights[i];
			
			if(light.inUse == false) {		
				var pos = getPosition(lat, lng, radius + 0.001);
				
				var c = new THREE.Color( color );
				light.color = c;
				
				light.position.x = pos.x;
				light.position.y = pos.y;
				light.position.z = pos.z;

				light.inUse = true;
				light.lifeTime = lifeTime;
				light.born = new Date().getTime();
				
				break;
			}		
		}
	}
	
	function updateLights() {
		var timeNow = new Date().getTime();
		
		for(var i = 0; i < lights.length; i++){
			var l = lights[i];
			
			if(l.inUse == true) {
				var lifeTime = l.lifeTime;
				var halfLife = lifeTime / 2;
				var age = timeNow - l.born;
			
				if(age < lifeTime) {
					var timeFromHalflife = Math.abs(age - halfLife);
			
					l.intensity =  10 * (1 - (timeFromHalflife / halfLife));
			
				}
				else {
					l.intensity = 0;
					l.inUse = false;
				}
			}			
		}
	}
	
	
	function render() {
		controls.update();
		
		updateLights();

		requestAnimationFrame(render);
		renderer.render(scene, camera);
	}
	


	// does the browser support web gl?
	if (!Detector.webgl) {
		Detector.addGetWebGLMessage(webglEl);
		return;
	}

	var width = window.innerWidth;
	var height = window.innerHeight;
	var radius = 1;
	var segments = 32;
	var rotation = 6;
	
	var scene = createScene();

	var earth = createEarth(radius, segments);
	earth.rotation.y = rotation;
	scene.add(earth);
	
	var clouds = createClouds(radius, segments);
	scene.add(clouds);
	
	var stars = createStars(radius * 210, 64);
	scene.add(stars);
	
	var sun = createSun(radius * 200);
	scene.add(sun);
	
	var lensFlare = createLensFlare(sun);
	scene.add(lensFlare);
	
	var lights = addRandomLights();
	
	// create the camera
	var camera = new THREE.PerspectiveCamera(45, width / height, 0.01, 1000);
	camera.position.z = 3;
	
	// create controls
	var controls = new THREE.OrbitControls(camera);
	controls.autoRotate = true;
	
	// create renderer
	var renderer = new THREE.WebGLRenderer( { antialias: true, alpha: true } );
	renderer.setSize(width, height);
	var webglEl = document.getElementById('webgl');
	webglEl.appendChild(renderer.domElement);
	
	var socket = io();
	
	socket.on('tweet', function(coord) {
		startLight(coord.lat, coord.lng, 2000, 0x6DAEE1);
	});

	render();
	

}());