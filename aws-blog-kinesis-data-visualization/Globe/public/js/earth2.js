(function() {
	
	// converts a point at (lat, lng) and a given altitue above the earth
	// to a point in 3d space.  (0,0,0) is the centre of the earth and
	// an altitude of 1 is the surface of the earth
	function getPosition(lat, lng, alt){
		
		var pos = {};
		
		pos.y = (lat / 90) * (earthHeight / 2);
		pos.x = (lng / 180) * (earthWidth / 2);
		pos.z = alt;
		
		
		return pos;
	}
	
	function createScene(){
		// create the scene
		var scene = new THREE.Scene();		

		return scene
	}
	
	function createEarth(width, height) {
		return new THREE.Mesh(new THREE.PlaneGeometry( width, height )
		   , new THREE.MeshPhongMaterial({
			map : THREE.ImageUtils.loadTexture('images/2_no_clouds_4k.jpg')
			}));
	}
	
	function createBackgroundLight(){
		return new THREE.AmbientLight( 0x999999 ); // soft white light
	}
	
	
	
	function createPointCloud() {
		attributes = {

			size: {	type: 'f', value: [] },
			customColor: { type: 'c', value: [] },
			lifeTime: { type: 'f', value: []}
		};

		var uniforms = {

			amplitude: { type: "f", value: 1.0 },
			color:     { type: "c", value: new THREE.Color( 0xffffff ) },
			texture:   { type: "t", value: THREE.ImageUtils.loadTexture( "images/spark.png" ) },

		};

		var shaderMaterial = new THREE.ShaderMaterial( {

			uniforms:       uniforms,
			attributes:     attributes,
			vertexShader:   document.getElementById( 'vertexshader' ).textContent,
			fragmentShader: document.getElementById( 'fragmentshader' ).textContent,

			blending:       THREE.AdditiveBlending,
			depthTest:      false,
			transparent:    true

		});
		
		var geometry = new THREE.Geometry();
		
		values_size = attributes.size.value;
		values_color = attributes.customColor.value;

		for ( var i = 0; i < countParticles; i ++ ) {
			
			var vertex = new THREE.Vector3();
			vertex.x = Number.POSITIVE_INFINITY;
			vertex.y = Number.POSITIVE_INFINITY;
			vertex.z = Number.POSITIVE_INFINITY;

			geometry.vertices.push( vertex );
			
			values_size[ i ] = 0.1;
			values_color[ i ] = new THREE.Color( 0xffaa00 );

		}

		var pointCloud = new THREE.PointCloud( geometry, shaderMaterial );

		return pointCloud;
	}
	
	function startLight(lat, lng, lifeTime){
		var timeNow = new Date().getTime();
		var index = -1;
		for ( var i = 0; i < countParticles; i ++ ) {
			if(attributes.lifeTime.value[i] == undefined || timeNow > attributes.lifeTime.value[i]) {
				index = i;
				
				if(index > maxParticles){
					console.log(index);
					maxParticles = index;
				}
				
				break;
			}
		}
		
		if(index > -1){
			attributes.lifeTime.value[index] = timeNow + lifeTime;
			
			var pos = getPosition(lat, lng, 0.02);
			
			pointCloud.geometry.vertices[index].x = pos.x;
			pointCloud.geometry.vertices[index].y = pos.y;
			pointCloud.geometry.vertices[index].z = pos.z;
			
		}
	}

	function render() {
		controls.update();
		
		var timeNow = new Date().getTime();

		for ( var i = 0; i < countParticles; i ++ ) {
			if(timeNow > attributes.lifeTime.value[i]) {
				pointCloud.geometry.vertices[i].x = Number.POSITIVE_INFINITY;
				pointCloud.geometry.vertices[i].y = Number.POSITIVE_INFINITY;
				pointCloud.geometry.vertices[i].z = Number.POSITIVE_INFINITY;
			}
		}

		pointCloud.geometry.verticesNeedUpdate = true;

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
	var earthWidth = 4;
	var earthHeight = 2;
	var values_size, values_color;
	var countParticles = 50000;
	var attributes;
	var maxParticles = 0;
	
	var scene = createScene();

	var earth = createEarth(earthWidth, earthHeight);
	scene.add(earth);
	
	var light = createBackgroundLight();
	scene.add(light);
	
	var pointCloud = createPointCloud();	
	scene.add(pointCloud);
	
	// create the camera
	var camera = new THREE.PerspectiveCamera(45, width / height, 0.01, 1000);
	camera.position.z = 2.5;
	camera.position.y = -1;
	
	// create controls
	var controls = new THREE.OrbitControls(camera);
	controls.noRotate = true;
	
	// create renderer
	var renderer = new THREE.WebGLRenderer();
	renderer.setSize(width, height);
	var webglEl = document.getElementById('webgl');
	webglEl.appendChild(renderer.domElement);
	
	var socket = io();
	
	socket.on('tweet', function(coord) {
		startLight(coord.lat, coord.lng, 10000);
	});

	render();
	

}());