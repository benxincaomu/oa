import * as React from "react";

import Grid from "@mui/material/Grid";
import Container from '@mui/material/Container';


export default function Home() {
  return (
    <Grid container spacing={1} sx={{ height: '100vh' }}>
      <Grid size={12} sx={{backgroundColor:"blue",height:"4vh",width:"100%"}}>
        <Container >size=12</Container>
        
      </Grid>
      <Grid size={2} sx={{height:"89vh"}}>
        size=4
      </Grid>
      <Grid size={10}>
        size=4
      </Grid>
      <Grid size={12} sx={{height:"4vh",backgroundColor:"#ffffff"}}>
        bootom
      </Grid>
    </Grid>
  );
}
